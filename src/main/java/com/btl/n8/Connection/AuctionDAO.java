package com.btl.n8.Connection;

import com.btl.n8.Controller.BidController.ItemAuctionRow;
import com.btl.n8.Model.Entity.Auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AuctionDAO {
    boolean insert(Auction auction);
    Auction findByItemId(int itemId);
    Auction findById(int id);
    boolean update(Auction auction);
    boolean updateCurrentPrice(int auctionId, BigDecimal price);
    List<Auction> findAll();
    boolean deleteById(int id);

    /**
     * Anti-sniping: gia hạn end_time cho auction.
     * Chỉ update nếu auction vẫn đang OPEN.
     */
    boolean extendEndTime(int auctionId, LocalDateTime newEndTime);

    /**
     * JOIN query: lấy tất cả item kèm thông tin auction (nếu có).
     */
    List<ItemAuctionRow> findAllWithItems();

    /**
     * Đóng tất cả auction OPEN đã quá end_time.
     * @return số auction vừa được đóng
     */
    int closeExpiredAuctions();
}