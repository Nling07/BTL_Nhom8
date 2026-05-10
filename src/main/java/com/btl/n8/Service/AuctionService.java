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
        if (auction == null) return false;
        if (auction.getStatus() != AuctionStatus.OPEN) return false;

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auction.getStartTime()) || now.isAfter(auction.getEndTime())) {
            return false;
        }
        if (newPrice.compareTo(auction.getCurrentPrice()) <= 0) {
            return false;
        }
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
