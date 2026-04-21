package com.btl.n8.Connection;

import com.btl.n8.Model.Bid;
import com.btl.n8.Model.BidStatus;

import java.util.List;

public interface BidDAO {
    boolean insert(Bid bid);
    List<Bid> findByAuction(int auctionId);
    Bid findHighestBid(int auctionId);
    boolean updateStatus(int bidId, BidStatus status);
    boolean updateOutbid(int auctionId);
}
