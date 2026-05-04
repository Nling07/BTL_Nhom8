package com.btl.n8.Service;

import com.btl.n8.Connection.BidDAO;
import com.btl.n8.Model.Bid;
import com.btl.n8.Model.BidStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BidService {
    private final BidDAO bidDAO;

    public BidService(BidDAO bidDAO) {
        this.bidDAO = bidDAO;
    }

    public boolean placeBid(int auctionId, int bidderId, BigDecimal amount) {
        Bid highest = bidDAO.findHighestBid(auctionId);
        if (highest != null && amount.compareTo(highest.getAmount()) <= 0) {
            return false;
        }

        Bid newBid = new Bid();
        newBid.setAuctionId(auctionId);
        newBid.setBidderId(bidderId);
        newBid.setAmount(amount);
        newBid.setBidTime(LocalDateTime.now());
        newBid.setStatus(BidStatus.ACTIVE);

        bidDAO.updateOutbid(auctionId);
        return bidDAO.insert(newBid);
    }

    public List<Bid> getBidsByAuction(int auctionId) {
        return bidDAO.findByAuction(auctionId);
    }

    public Bid getHighestBid(int auctionId) {
        return bidDAO.findHighestBid(auctionId);
    }

    public boolean updateBidStatus(int bidId, BidStatus status) {
        return bidDAO.updateStatus(bidId, status);
    }

    public boolean markWinner(int auctionId) {
        Bid highest = bidDAO.findHighestBid(auctionId);
        if (highest != null) {
            return bidDAO.updateStatus(highest.getId(), BidStatus.WINNER);
        }
        return false;
    }
}
