package com.btl.n8.DTO;

import java.math.BigDecimal;

/**
 * DTO dùng cho kết quả JOIN query giữa items và auctions.
 * Tách khỏi BidController để AuctionDAO không phụ thuộc vào tầng Controller.
 */
public class ItemAuctionRow {
    public int        itemId;
    public String     itemName;
    public String     itemType;
    public BigDecimal currentPrice;
    public String     status;
    public int        auctionId;
    public int        sellerId;

    public ItemAuctionRow() {}

    public ItemAuctionRow(int itemId, String itemName, String itemType,
                          BigDecimal currentPrice, String status,
                          int auctionId, int sellerId) {
        this.itemId       = itemId;
        this.itemName     = itemName;
        this.itemType     = itemType;
        this.currentPrice = currentPrice;
        this.status       = status;
        this.auctionId    = auctionId;
        this.sellerId     = sellerId;
    }
}
