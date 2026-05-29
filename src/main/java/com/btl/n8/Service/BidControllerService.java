package com.btl.n8.Service;

import com.btl.n8.DTO.GetAuctionDetailRequest;
import com.btl.n8.DTO.GetAuctionListRequest;
import com.btl.n8.Network.ClientSocket;

/**
 * Service xử lý logic cho BidController.
 * Controller chỉ gọi các method ở đây — không tự gửi request hay xử lý data.
 */
public class BidControllerService {

    /**
     * Gửi request lấy danh sách auction (tất cả item kèm thông tin đấu giá).
     *
     * @param sessionId session ID của người dùng hiện tại
     */
    public void requestAuctionList(String sessionId) {
        ClientSocket.getInstance().sendMessage(new GetAuctionListRequest(sessionId));
    }

    /**
     * Gửi request lấy chi tiết một auction cụ thể.
     *
     * @param sessionId session ID của người dùng hiện tại
     * @param auctionId ID của auction cần lấy chi tiết
     */
    public void requestAuctionDetail(String sessionId, int auctionId) {
        ClientSocket.getInstance().sendMessage(new GetAuctionDetailRequest(sessionId, auctionId));
    }

    /**
     * Kiểm tra xem người dùng có phải chủ của item trong hàng này không.
     *
     * @param rowSellerId seller_id của hàng trong bảng
     * @param currentUserId ID của người dùng hiện tại
     * @return true nếu người dùng là chủ item
     */
    public boolean isOwnItem(int rowSellerId, int currentUserId) {
        return rowSellerId == currentUserId;
    }

    /**
     * Kiểm tra xem auction có đang mở không và có thể đặt giá không.
     *
     * @param auctionId auctionId trong hàng (-1 nếu không có auction)
     * @param status    trạng thái auction
     * @return true nếu auction đang OPEN
     */
    public boolean isAuctionOpen(int auctionId, String status) {
        return auctionId != -1 && "OPEN".equals(status);
    }
}
