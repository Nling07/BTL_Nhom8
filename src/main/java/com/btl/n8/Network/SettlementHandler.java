package com.btl.n8.Network;

import com.btl.n8.Connection.*;
import com.btl.n8.DTO.AuctionSettledResponse;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Bid;
import com.btl.n8.Model.Entity.User;
import com.btl.n8.Model.Enums.AuctionStatus;
import com.btl.n8.Model.Enums.BidStatus;
import com.btl.n8.Service.*;
import com.btl.n8.Util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Xử lý kết thúc phiên đấu giá:
 *   1. Đóng auction (OPEN → CLOSED)
 *   2. Xác định winner = bid WINNER trong bảng bids (bid cao nhất)
 *   3. settleWinner: trừ balance thật + giải phóng frozen (atomic 1 SQL)
 *   4. Unfreeze cho tất cả người không thắng
 *   5. Broadcast AUCTION_SETTLED tới tất cả client
 *
 * Dependency Injection: dùng ServiceFactory thay vì tự new DAO.
 * Thread-safe: per-auction ReentrantLock + idempotent guard.
 */
public class SettlementHandler {

    private final ConcurrentHashMap<String, ClientHandler> clients;

    private static final ConcurrentHashMap<Integer, ReentrantLock> settleLocks =
            new ConcurrentHashMap<>();

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public SettlementHandler(ConcurrentHashMap<String, ClientHandler> clients) {
        this.clients = clients;
    }

    private ReentrantLock getSettleLock(int auctionId) {
        return settleLocks.computeIfAbsent(auctionId, id -> new ReentrantLock(true));
    }

    public void settleAuction(int auctionId) {
        ReentrantLock lock = getSettleLock(auctionId);

        if (!lock.tryLock()) {
            System.out.println("[Settlement] Auction " + auctionId + " đang được settle bởi luồng khác, bỏ qua.");
            return;
        }

        try {
            Connection conn = DataConnection.getConnection();
            if (conn == null) return;

            // Dùng ServiceFactory — không còn new DAOImpl trực tiếp
            AuctionService auctionService = ServiceFactory.createAuctionService(conn);
            BidService     bidService     = ServiceFactory.createBidService(conn);
            UserService    userService    = ServiceFactory.createUserService(conn);

            Auction auction = auctionService.getAuctionById(auctionId);
            if (auction == null) return;

            // Idempotent: đã settled thì bỏ qua
            if (auction.getStatus() != AuctionStatus.OPEN) {
                System.out.println("[Settlement] Auction " + auctionId + " đã CLOSED, bỏ qua.");
                return;
            }

            // 1. Đóng auction
            auctionService.closeAuction(auctionId);

            // 2. Xác định winner = bid cao nhất (findHighestBid)
            //    → mark bid đó là WINNER trong bảng bids
            //    → các bid còn lại mark OUTBID
            Bid highestBid = bidService.getHighestBid(auctionId);

            int        winnerId      = -1;
            String     winnerAccount = null;
            BigDecimal winningPrice  = auction.getCurrentPrice();
            BigDecimal winnerNewBal  = null;

            if (highestBid != null) {
                winnerId = highestBid.getBidderId();

                // Mark tất cả bid ACTIVE → OUTBID trước
                bidService.markAllOutbid(auctionId);
                // Mark riêng bid thắng là WINNER
                bidService.updateBidStatus(highestBid.getId(), BidStatus.WINNER);

                User winner = userService.getUserById(winnerId);
                if (winner != null) {
                    winnerAccount = winner.getAccount();

                    // 3. settleWinner: atomic — trừ balance thật + giải phóng frozen 1 lần
                    boolean settled = userService.settleWinner(winnerId, highestBid.getAmount());
                    if (settled) {
                        winner = userService.getUserById(winnerId); // reload balance mới
                        System.out.println("[Settlement] settleWinner " + winnerAccount
                                + ": trừ " + highestBid.getAmount()
                                + " → balance còn: " + (winner != null ? winner.getBalance() : "?"));
                    } else {
                        System.err.println("[Settlement] settleWinner thất bại cho user " + winnerId);
                    }
                }

                winnerNewBal = (winner != null) ? winner.getBalance() : BigDecimal.ZERO;
                winningPrice = highestBid.getAmount();

                // 4. Unfreeze cho tất cả người không thắng
                unfreezeNonWinners(auctionId, winnerId, bidService, userService);
            } else {
                // Không có bid nào → unfreeze tất cả (trường hợp lý thuyết, bảo vệ data)
                unfreezeNonWinners(auctionId, -1, bidService, userService);
            }

            // 5. Broadcast AUCTION_SETTLED
            final int        fWinnerId    = winnerId;
            final String     fAccount     = winnerAccount;
            final BigDecimal fPrice       = winningPrice;
            final BigDecimal fNewBalance  = winnerNewBal;

            for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
                ClientHandler ch = entry.getValue();
                String loginSid = ch.getLoginSessionId();
                User sessionUser = loginSid != null
                        ? ServerSessionManager.getInstance().getUser(loginSid)
                        : null;

                AuctionSettledResponse resp = new AuctionSettledResponse(
                        loginSid, auctionId, fWinnerId, fAccount, fPrice);
                if (sessionUser != null && sessionUser.getId() == fWinnerId) {
                    resp.setIsWinner(true);
                    resp.setNewBalance(fNewBalance);
                }
                ch.send(gson.toJson(resp));
            }

            System.out.println("[Settlement] Auction " + auctionId + " settled."
                    + (winnerId != -1 ? " Winner=" + winnerAccount + " price=" + winningPrice
                    : " No bids."));

        } catch (Exception e) {
            System.err.println("[Settlement] Lỗi settle auction " + auctionId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Unfreeze balance cho tất cả bidder không thắng trong auction.
     * Mỗi bidder: tìm bid cao nhất của họ (= số tiền đang bị frozen) rồi unfreeze.
     */
    private void unfreezeNonWinners(int auctionId, int winnerId,
                                    BidService bidService, UserService userService) {
        List<Bid> allBids = bidService.getBidsByAuction(auctionId);

        // Map bidderId → bid amount cao nhất của người đó (= tiền đang frozen)
        java.util.Map<Integer, BigDecimal> frozenPerBidder = new java.util.HashMap<>();
        for (Bid b : allBids) {
            if (b.getBidderId() == winnerId) continue; // winner đã settleWinner rồi
            frozenPerBidder.merge(b.getBidderId(), b.getAmount(),
                    (old, nw) -> old.compareTo(nw) >= 0 ? old : nw);
        }

        for (Map.Entry<Integer, BigDecimal> entry : frozenPerBidder.entrySet()) {
            boolean ok = userService.unfreezeBalance(entry.getKey(), entry.getValue());
            if (ok) {
                System.out.println("[Settlement] Unfreeze " + entry.getValue()
                        + " cho bidder " + entry.getKey());
            }
        }
    }
}