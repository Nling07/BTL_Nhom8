package com.btl.n8.Service;

import com.btl.n8.DTO.BidRequest;
import com.btl.n8.DTO.GetAuctionDetailRequest;
import com.btl.n8.DTO.GetUserBalanceRequest;
import com.btl.n8.DTO.GetUserBidsRequest;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.User;
import com.btl.n8.Model.Enums.AuctionStatus;
import com.btl.n8.Network.ClientSocket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service xử lý toàn bộ logic nghiệp vụ cho BidDetailController.
 * Controller chỉ gọi các method này và cập nhật UI dựa trên kết quả trả về.
 */
public class BidDetailControllerService {

    // ── Request gửi đi ────────────────────────────────────────────────────────

    /**
     * Gửi request lấy chi tiết auction và lịch sử đấu giá.
     */
    public void requestAuctionDetail(String sessionId, int auctionId) {
        ClientSocket.getInstance().sendMessage(new GetAuctionDetailRequest(sessionId, auctionId));
    }

    /**
     * Gửi request lấy balance mới nhất từ server (sau unfreeze / settle).
     * Dùng GET_USER_BALANCE — server reload từ DB và trả về USER_BALANCE_RESULT.
     */
    public void requestUserBalance(String sessionId, int userId) {
        ClientSocket.getInstance().sendMessage(new GetUserBalanceRequest(sessionId, userId));
    }

    // ── Validate bid ──────────────────────────────────────────────────────────

    /**
     * Kết quả validate của một lần đặt giá.
     * Nếu valid == false thì errorMessage chứa lý do lỗi.
     */
    public static class BidValidationResult {
        public final boolean valid;
        public final String  errorMessage;
        public final BigDecimal amount;

        private BidValidationResult(boolean valid, String errorMessage, BigDecimal amount) {
            this.valid        = valid;
            this.errorMessage = errorMessage;
            this.amount       = amount;
        }

        public static BidValidationResult ok(BigDecimal amount) {
            return new BidValidationResult(true, null, amount);
        }

        public static BidValidationResult fail(String msg) {
            return new BidValidationResult(false, msg, null);
        }
    }

    /**
     * Kiểm tra tất cả điều kiện trước khi đặt giá.
     *
     * @param inputText   chuỗi nhập từ TextField
     * @param auction     auction hiện tại
     * @param currentUser user đang đặt giá
     * @return BidValidationResult chứa kết quả và amount đã parse (nếu hợp lệ)
     */
    public BidValidationResult validateBid(String inputText, Auction auction, User currentUser) {
        if (inputText == null || inputText.isBlank()) {
            return BidValidationResult.fail("Please enter a bid amount");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(inputText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return BidValidationResult.fail("Amount must be positive");
            }
        } catch (NumberFormatException e) {
            return BidValidationResult.fail("Invalid amount format");
        }

        if (auction == null) {
            return BidValidationResult.fail("Auction not available");
        }

        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            return BidValidationResult.fail("Auction has ended!");
        }

        if (auction.getStatus() != AuctionStatus.OPEN) {
            return BidValidationResult.fail("Auction is not open!");
        }

        if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
            return BidValidationResult.fail(
                    "Bid must be higher than " + formatMoney(auction.getCurrentPrice()));
        }

        if (currentUser != null) {
            BigDecimal available = currentUser.getAvailableBalance();
            if (amount.compareTo(available) > 0) {
                BigDecimal frozen = currentUser.getFrozenBalance() != null
                        ? currentUser.getFrozenBalance() : BigDecimal.ZERO;
                return BidValidationResult.fail(String.format(
                        "Số dư khả dụng không đủ! Khả dụng: %s (đang khóa: %s) — Cần: %s",
                        formatMoney(available), formatMoney(frozen), formatMoney(amount)));
            }
        }

        return BidValidationResult.ok(amount);
    }

    /**
     * Gửi BidRequest lên server sau khi đã validate thành công.
     *
     * @param auctionId auction ID
     * @param bidderId  bidder ID
     * @param amount    số tiền đặt giá
     * @param sessionId session ID
     */
    public void sendBidRequest(int auctionId, int bidderId, BigDecimal amount, String sessionId) {
        BidRequest req = new BidRequest(auctionId, bidderId, amount);
        req.setSessionId(sessionId);
        ClientSocket.getInstance().sendMessage(req);
    }

    // ── Business logic phụ trợ ────────────────────────────────────────────────

    /**
     * Kiểm tra xem bid có thuộc về user hiện tại không.
     *
     * @param responseBidderId bidderId từ server response
     * @param currentBidderId  bidderId của user hiện tại
     * @return true nếu là bid của user hiện tại
     */
    public boolean isMyBid(int responseBidderId, int currentBidderId) {
        return responseBidderId == currentBidderId;
    }

    /**
     * Kiểm tra xem response auction settlement có phải của auction đang xem không.
     *
     * @param responseAuctionId auctionId từ server response
     * @param currentAuctionId  auctionId đang xem
     */
    public boolean isCurrentAuction(int responseAuctionId, int currentAuctionId) {
        return responseAuctionId == currentAuctionId;
    }

    /**
     * Cập nhật balance của user khi họ thắng auction.
     *
     * @param user       đối tượng User cần cập nhật
     * @param newBalance balance mới sau khi thanh toán
     */
    public void applyWinnerBalance(User user, BigDecimal newBalance) {
        if (user == null) return;
        user.setBalance(newBalance);
        user.setFrozenBalance(BigDecimal.ZERO);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    public String formatMoney(BigDecimal amount) {
        return String.format("%,.0f ₫", amount);
    }
}