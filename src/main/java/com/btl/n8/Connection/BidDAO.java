package com.btl.n8.Connection;

import com.btl.n8.Model.entity.Bid;
import com.btl.n8.Model.enums.BidStatus;

import java.util.List;

public interface BidDAO {
    boolean insert(Bid bid);
    List<Bid> findByAuction(int auctionId);
    List<Bid> findByBidder(int bidderId); // thêm
    Bid findHighestBid(int auctionId);
    boolean updateStatus(int bidId, BidStatus status);
    boolean updateOutbid(int auctionId);
    boolean deleteById(int id);
}