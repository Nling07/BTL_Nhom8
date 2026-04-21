package com.btl.n8.Connection;

import com.btl.n8.Model.Auction;

import java.math.BigDecimal;

public interface AuctionDAO {
    boolean insert(Auction auction);
    Auction findByItemId(int itemId);
    Auction findById(int id);
    boolean update(Auction auction);
    boolean updateCurrentPrice(int auctionId, BigDecimal price);

}
