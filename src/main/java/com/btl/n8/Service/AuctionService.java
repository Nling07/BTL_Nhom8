package com.btl.n8.Service;

import com.btl.n8.Connection.AuctionDAO;
import com.btl.n8.Model.Auction;
import com.btl.n8.Model.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {

    private final AuctionDAO auctionDAO;

    public AuctionService(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }

    public boolean createAuction(Auction auction) {

        if (auction.getStartTime().isAfter(auction.getEndTime())) {
            return false;
        }

        auction.setStatus(AuctionStatus.OPEN);

        return auctionDAO.insert(auction);
    }

    public boolean placeBid(int auctionId, BigDecimal newPrice) {

        Auction auction = auctionDAO.findById(auctionId);

        if (auction == null) {
            return false;
        }

        // Nếu auction không mở
        if (auction.getStatus() != AuctionStatus.OPEN) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        // Chưa tới giờ mở
        if (now.isBefore(auction.getStartTime())) {
            return false;
        }

        // Đã hết giờ -> auto đóng auction
        if (now.isAfter(auction.getEndTime())) {

            auction.setStatus(AuctionStatus.CLOSED);

            auctionDAO.update(auction);

            return false;
        }

        // Giá bid phải lớn hơn current price
        if (newPrice.compareTo(auction.getCurrentPrice()) <= 0) {
            return false;
        }

        return auctionDAO.updateCurrentPrice(
                auctionId,
                newPrice
        );
    }

    public boolean closeAuction(int auctionId) {

        Auction auction = auctionDAO.findById(auctionId);

        if (auction == null) {
            return false;
        }

        auction.setStatus(AuctionStatus.CLOSED);

        return auctionDAO.update(auction);
    }

    public boolean cancelAuction(int auctionId) {

        Auction auction = auctionDAO.findById(auctionId);

        if (auction == null) {
            return false;
        }

        auction.setStatus(AuctionStatus.CANCELLED);

        return auctionDAO.update(auction);
    }

    public Auction getAuctionById(int id) {

        Auction auction = auctionDAO.findById(id);

        // Auto update CLOSED nếu quá giờ
        if (auction != null
                && auction.getStatus() == AuctionStatus.OPEN
                && LocalDateTime.now().isAfter(auction.getEndTime())) {

            auction.setStatus(AuctionStatus.CLOSED);

            auctionDAO.update(auction);
        }

        return auctionDAO.findById(id);
    }

    public Auction getAuctionByItemId(int itemId) {

        Auction auction = auctionDAO.findByItemId(itemId);

        // Auto update CLOSED nếu quá giờ
        if (auction != null
                && auction.getStatus() == AuctionStatus.OPEN
                && LocalDateTime.now().isAfter(auction.getEndTime())) {

            auction.setStatus(AuctionStatus.CLOSED);

            auctionDAO.update(auction);
        }

        return auctionDAO.findByItemId(itemId);
    }

    public List<Auction> getAllAuctions() {

        List<Auction> auctions = auctionDAO.findAll();

        for (Auction auction : auctions) {

            if (auction.getStatus() == AuctionStatus.OPEN
                    && LocalDateTime.now().isAfter(auction.getEndTime())) {

                auction.setStatus(AuctionStatus.CLOSED);

                auctionDAO.update(auction);
            }
        }

        return auctionDAO.findAll();
    }
}