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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Xử lý việc kết thúc phiên đấu giá:
 *   1. Đóng auction (OPEN → CLOSED)
 *   2. Xác định winner (bid cao nhất)
 *   3. Trừ balance của winner
 *   4. Mark OUTBID tất cả bid còn lại, rồi mark bid cao nhất là WINNER
 *   5. Broadcast AUCTION_SETTLED tới tất cả client
 *
 * Tách khỏi RequestHandler để Server1 scheduler có thể tái dùng.
 */
public class SettlementHandler {

    private final ConcurrentHashMap<String, ClientHandler> clients;

    // FIX: per-auction lock để tránh double-settle khi scheduler và handleBid() cùng trigger
    private static final ConcurrentHashMap<Integer, ReentrantLock> settleLocks =
            new ConcurrentHashMap<>();

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public SettlementHandler(ConcurrentHashMap<String, ClientHandler> clients) {
        this.clients = clients;
    }

    /**
     * Trừ balance bằng UPDATE SQL trực tiếp — atomic, không cần load object trước.
     * Áp dụng cho cả Bidder lẫn Seller-upgrader (cùng dùng bảng bidders).
     * Dùng GREATEST(..., 0) để tránh balance âm.
     */
    private boolean updateBalanceDirect(Connection conn, int userId, BigDecimal amount) {
        String sql = "UPDATE bidders SET balance = GREATEST(balance - ?, 0) WHERE user_id = ?";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            System.err.println("[Settlement] updateBalanceDirect lỗi: " + e.getMessage());
            return false;
        }
    }

    private ReentrantLock getSettleLock(int auctionId) {
        return settleLocks.computeIfAbsent(auctionId, id -> new ReentrantLock(true));
    }

    /**
     * Settle auction. Idempotent + thread-safe:
     * chỉ một luồng được settle tại một thời điểm nhờ per-auction ReentrantLock.
     */
    public void settleAuction(int auctionId) {
        ReentrantLock lock = getSettleLock(auctionId);

        // Không block nếu luồng khác đang settle auction này
        if (!lock.tryLock()) {
            System.out.println("[Settlement] Auction " + auctionId + " đang được settle bởi luồng khác, bỏ qua.");
            return;
        }

        try {
            Connection conn = DataConnection.getConnection();
            if (conn == null) return;

            AuctionService auctionService = new AuctionService(new AuctionDAOImpl(conn));
            BidService     bidService     = new BidService(new BidDAOImpl(conn));
            UserService    userService    = new UserService(new UserDAOImpl(conn));

            Auction auction = auctionService.getAuctionById(auctionId);
            if (auction == null) return;

            // Idempotent guard: đã settled rồi thì bỏ qua
            if (auction.getStatus() != AuctionStatus.OPEN) {
                System.out.println("[Settlement] Auction " + auctionId + " đã CLOSED, bỏ qua.");
                return;
            }

            // 1. Đóng auction ngay — ngăn luồng khác vào sau khi release lock
            auctionService.closeAuction(auctionId);

            // 2. Tìm winner
            Bid highestBid = bidService.getHighestBid(auctionId);

            int        winnerId         = -1;
            String     winnerAccount    = null;
            BigDecimal winningPrice     = auction.getCurrentPrice();
            BigDecimal winnerNewBalance = null; // balance mới của winner sau khi bị trừ

            if (highestBid != null) {
                winnerId = highestBid.getBidderId();

                User winner = userService.getUserById(winnerId);
                if (winner != null) {
                    winnerAccount = winner.getAccount();

                    // 3. Trừ balance bằng UPDATE trực tiếp (atomic, tránh race condition)
                    // Dùng GREATEST(balance - amount, 0) để tránh balance âm.
                    boolean balanceUpdated = updateBalanceDirect(conn, winnerId, highestBid.getAmount());
                    if (balanceUpdated) {
                        // Reload user sau khi update để có balance mới nhất
                        winner = userService.getUserById(winnerId);
                        System.out.println("[Settlement] Trừ " + highestBid.getAmount()
                                + " từ " + winnerAccount
                                + " → balance còn: " + (winner != null ? winner.getBalance() : "?"));
                    } else {
                        // Fallback: update qua object
                        BigDecimal bal = winner.getBalance() != null ? winner.getBalance() : BigDecimal.ZERO;
                        BigDecimal newBal = bal.subtract(highestBid.getAmount());
                        if (newBal.compareTo(BigDecimal.ZERO) < 0) newBal = BigDecimal.ZERO;
                        winner.setBalance(newBal);
                        userService.updateUser(winner);
                        System.out.println("[Settlement] Trừ (fallback) " + highestBid.getAmount()
                                + " từ " + winnerAccount + " → balance còn: " + newBal);
                    }
                }

                // Lưu lại balance mới nhất của winner để gửi về client
                winnerNewBalance = (winner != null) ? winner.getBalance() : BigDecimal.ZERO;
                winningPrice = highestBid.getAmount();

                // 4a. Mark tất cả bid ACTIVE → OUTBID
                bidService.markAllOutbid(auctionId);
                // 4b. Mark riêng bid thắng là WINNER
                bidService.updateBidStatus(highestBid.getId(), BidStatus.WINNER);
            }

            // 5. Broadcast AUCTION_SETTLED
            final int        fWinnerId     = winnerId;
            final String     fAccount      = winnerAccount;
            final BigDecimal fPrice        = winningPrice;
            final BigDecimal fNewBalance   = winnerNewBalance;

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
                    // Gửi balance mới về cho winner để client cập nhật SessionManager
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
}