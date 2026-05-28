package com.btl.n8.Service;

import com.btl.n8.Connection.AuctionDAO;
import com.btl.n8.Connection.ItemDAO;
import com.btl.n8.Connection.UserDAO;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Item;
import com.btl.n8.Model.Entity.User;
import com.btl.n8.Model.Enums.AuctionStatus;
import com.btl.n8.Model.Enums.Role;

import java.util.List;
import java.util.stream.Collectors;

public class AdminService {

    private final UserDAO    userDAO;
    private final AuctionDAO auctionDAO;
    private final ItemDAO    itemDAO;

    public AdminService(UserDAO userDAO, AuctionDAO auctionDAO, ItemDAO itemDAO) {
        this.userDAO    = userDAO;
        this.auctionDAO = auctionDAO;
        this.itemDAO    = itemDAO;
    }

    // ===================== QUẢN LÝ USER =====================

    /** Lấy toàn bộ danh sách user (trừ ADMIN). */
    public List<User> getAllUsers() {
        return userDAO.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .collect(Collectors.toList());
    }

    /** Lấy thông tin user theo id. */
    public User getUserById(int id) {
        return userDAO.findById(id);
    }

    /**
     * Nâng cấp user lên SELLER (dùng upgradeToSeller đã có trong UserDAO).
     * Trả về false nếu đã là SELLER hoặc không tồn tại.
     */
    public boolean upgradeUserToSeller(int userId) {
        return userDAO.upgradeToSeller(userId);
    }

    /**
     * Hạ cấp SELLER về BIDDER (thu hồi quyền bán).
     * Dùng setRoleById đã có trong UserDAO.
     */
    public boolean demoteSellerToBidder(int userId) {
        User user = userDAO.findById(userId);
        if (user == null || user.getRole() != Role.SELLER) return false;
        return userDAO.setRoleById(userId, Role.BIDDER);
    }

    /**
     * Xóa user khỏi hệ thống.
     * Lưu ý: xóa user sẽ cascade xóa các bản ghi liên quan
     * (bid, auction nếu có FK ON DELETE CASCADE trong DB).
     */
    public boolean deleteUserById(int id) {
        return userDAO.deleteById(id);
    }

    // ===================== QUẢN LÝ AUCTION =====================

    /** Lấy toàn bộ danh sách phiên đấu giá. */
    public List<Auction> getAllAuctions() {
        return auctionDAO.findAll();
    }

    /** Lấy các phiên đang OPEN. */
    public List<Auction> getOpenAuctions() {
        return auctionDAO.findAll().stream()
                .filter(a -> a.getStatus() == AuctionStatus.OPEN)
                .collect(Collectors.toList());
    }

    /** Lấy các phiên đã CLOSED. */
    public List<Auction> getClosedAuctions() {
        return auctionDAO.findAll().stream()
                .filter(a -> a.getStatus() == AuctionStatus.CLOSED)
                .collect(Collectors.toList());
    }

    /** Lấy thông tin 1 phiên theo id. */
    public Auction getAuctionById(int id) {
        return auctionDAO.findById(id);
    }

    /**
     * Admin đóng sớm 1 phiên đang OPEN.
     * Dùng update(Auction) đã có trong AuctionDAO.
     */
    public boolean closeAuction(int auctionId) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) return false;
        if (auction.getStatus() != AuctionStatus.OPEN) return false;
        auction.setStatus(AuctionStatus.CLOSED);
        return auctionDAO.update(auction);
    }

    /**
     * Admin hủy phiên đấu giá (CANCELLED).
     * Khác với close: CANCELLED nghĩa là không hợp lệ / vi phạm.
     */
    public boolean cancelAuction(int auctionId) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) return false;
        auction.setStatus(AuctionStatus.CANCELLED);
        return auctionDAO.update(auction);
    }

    /**
     * Xóa cứng 1 phiên đấu giá.
     * Chỉ nên dùng cho phiên CANCELLED hoặc test data.
     */
    public boolean deleteAuctionById(int id) {
        return auctionDAO.deleteById(id);
    }

    // ===================== QUẢN LÝ ITEM =====================

    /** Lấy toàn bộ danh sách vật phẩm. */
    public List<Item> getAllItems() {
        return itemDAO.findAll();
    }

    /** Lấy vật phẩm theo id. */
    public Item getItemById(int id) {
        return itemDAO.findById(id);
    }

    /** Lấy danh sách vật phẩm của 1 seller. */
    public List<Item> getItemsBySeller(int sellerId) {
        return itemDAO.findBySeller(sellerId);
    }

    /**
     * Admin xóa vật phẩm vi phạm.
     * Nếu item đang trong phiên đấu giá OPEN, nên closeAuction trước.
     */
    public boolean deleteItemById(int id) {
        return itemDAO.deleteById(id);
    }

    // ===================== THỐNG KÊ DASHBOARD =====================

    public int getTotalUsers() {
        return (int) userDAO.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .count();
    }

    public int getTotalAuctions() {
        return auctionDAO.findAll().size();
    }

    public int getOpenAuctionCount() {
        return (int) auctionDAO.findAll().stream()
                .filter(a -> a.getStatus() == AuctionStatus.OPEN)
                .count();
    }

    public int getCancelledAuctionCount() {
        return (int) auctionDAO.findAll().stream()
                .filter(a -> a.getStatus() == AuctionStatus.CANCELLED)
                .count();
    }

    public int getTotalItems() {
        return itemDAO.findAll().size();
    }

    public int getTotalSellers() {
        return (int) userDAO.findAll().stream()
                .filter(u -> u.getRole() == Role.SELLER)
                .count();
    }
}