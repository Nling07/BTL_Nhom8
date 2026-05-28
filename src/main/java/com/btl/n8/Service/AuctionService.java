package com.btl.n8.Service;

import com.btl.n8.Connection.AuctionDAO;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Enums.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {
    private final AuctionDAO auctionDAO;

    public AuctionService(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }

    /**
     * Expose DAO để BidController có thể cast sang AuctionDAOImpl
     * và gọi findAllWithItems() (JOIN query).
     */
    public AuctionDAO getAuctionDAO() {
        return auctionDAO;
    }

    public boolean createAuction(Auction auction) {
        if (auction.getStartTime().isAfter(auction.getEndTime())) return false;
        auction.setStatus(AuctionStatus.OPEN);
        return auctionDAO.insert(auction);
    }

    public boolean placeBid(int auctionId, BigDecimal newPrice) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) return false;
        if (auction.getStatus() != AuctionStatus.OPEN) return false;

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auction.getStartTime()) || now.isAfter(auction.getEndTime())) return false;
        if (newPrice.compareTo(auction.getCurrentPrice()) <= 0) return false;

        return auctionDAO.updateCurrentPrice(auctionId, newPrice);
    }

    public boolean closeAuction(int auctionId) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) return false;
        auction.setStatus(AuctionStatus.CLOSED);
        return auctionDAO.update(auction);
    }

    public boolean cancelAuction(int auctionId) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) return false;
        auction.setStatus(AuctionStatus.CANCELLED);
        return auctionDAO.update(auction);
    }

    /**
     * Anti-sniping: gia hạn thời gian auction thêm extraSeconds giây.
     * Chỉ có hiệu lực nếu auction vẫn OPEN.
     * @return endTime mới nếu gia hạn thành công, null nếu thất bại.
     */
    public LocalDateTime extendEndTime(int auctionId, int extraSeconds) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.OPEN) return null;

        LocalDateTime newEndTime = auction.getEndTime().plusSeconds(extraSeconds);
        boolean ok = auctionDAO.extendEndTime(auctionId, newEndTime);
        return ok ? newEndTime : null;
    }

    public Auction getAuctionById(int id) {
        return auctionDAO.findById(id);
    }

    public Auction getAuctionByItemId(int itemId) {
        return auctionDAO.findByItemId(itemId);
    }

    public List<Auction> getAllAuctions() {
        return auctionDAO.findAll();
    }
}