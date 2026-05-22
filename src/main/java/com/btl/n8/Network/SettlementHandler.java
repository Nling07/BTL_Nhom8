package com.btl.n8.Network;

import com.btl.n8.Connection.*;
import com.btl.n8.DTO.AuctionSettledResponse;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Bid;
import com.btl.n8.Model.Entity.User;
import com.btl.n8.Model.Enums.AuctionStatus;
import com.btl.n8.Model.Enums.BidStatus;
import com.btl.n8.Service.AuctionService;
import com.btl.n8.Service.BidService;
import com.btl.n8.Service.UserService;
import com.btl.n8.Util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Xử lý việc kết thúc phiên đấu giá:
 *   1. Đóng auction (OPEN → CLOSED)
 *   2. Xác định winner (bid cao nhất)
 *   3. Trừ balance của winner
 *   4. Mark bid WINNER / OUTBID
 *   5. Broadcast AUCTION_SETTLED tới tất cả client
 *
 * Tách khỏi RequestHandler để Server1 scheduler có thể tái dùng.
 */
public class SettlementHandler {

    private final ConcurrentHashMap<String, ClientHandler> clients;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public SettlementHandler(ConcurrentHashMap<String, ClientHandler> clients) {
        this.clients = clients;
    }

    /**
     * Settle auction. Idempotent: nếu auction đã CLOSED thì không làm gì thêm
     * (tránh double-settle khi cả scheduler lẫn bid handler cùng trigger).
     */
    public void settleAuction(int auctionId) {
        try {
            Connection conn = DataConnection.getConnection();
            if (conn == null) return;

            AuctionService auctionService = new AuctionService(new AuctionDAOImpl(conn));
            BidService     bidService     = new BidService(new BidDAOImpl(conn));
            UserService    userService    = new UserService(new UserDAOImpl(conn));

            Auction auction = auctionService.getAuctionById(auctionId);
            if (auction == null) return;

            // Idempotent guard: đã settled rồi thì bỏ qua
            if (auction.getStatus() != AuctionStatus.OPEN) return;

            // 1. Đóng auction
            auctionService.closeAuction(auctionId);

            // 2. Tìm winner
            Bid highestBid = bidService.getHighestBid(auctionId);

            int      winnerId      = -1;
            String   winnerAccount = null;
            BigDecimal winningPrice = auction.getCurrentPrice();

            if (highestBid != null) {
                winnerId = highestBid.getBidderId();

                User winner = userService.getUserById(winnerId);
                if (winner != null) {
                    winnerAccount = winner.getAccount();

                    // 3. Trừ balance
                    BigDecimal balance    = winner.getBalance() != null ? winner.getBalance() : BigDecimal.ZERO;
                    BigDecimal newBalance = balance.subtract(highestBid.getAmount());
                    if (newBalance.compareTo(BigDecimal.ZERO) < 0) newBalance = BigDecimal.ZERO;
                    winner.setBalance(newBalance);
                    userService.updateUser(winner);

                    System.out.println("[Settlement] Trừ " + highestBid.getAmount()
                            + " từ " + winnerAccount
                            + " → balance còn: " + newBalance);
                }

                winningPrice = highestBid.getAmount();

                // 4. Mark bid WINNER
                bidService.updateBidStatus(highestBid.getId(), BidStatus.WINNER);
            }

            // 5. Broadcast AUCTION_SETTLED
            final int      fWinnerId  = winnerId;
            final String   fAccount   = winnerAccount;
            final BigDecimal fPrice   = winningPrice;

            for (java.util.Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
                String sid = entry.getKey();
                ClientHandler ch = entry.getValue();

                User sessionUser = ServerSessionManager.getInstance().getUser(sid);
                AuctionSettledResponse resp = new AuctionSettledResponse(
                        sid, auctionId, fWinnerId, fAccount, fPrice);
                if (sessionUser != null && sessionUser.getId() == fWinnerId) {
                    resp.setIsWinner(true);
                }
                ch.send(gson.toJson(resp));
            }

            System.out.println("[Settlement] Auction " + auctionId + " settled."
                    + (winnerId != -1 ? " Winner=" + winnerAccount + " price=" + winningPrice
                    : " No bids."));

        } catch (Exception e) {
            System.err.println("[Settlement] Lỗi settle auction " + auctionId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}