package com.btl.n8.DTO;

import java.math.BigDecimal;
import java.util.List;

public class GetAuctionListResponse extends Response {
    private boolean success;
    private List<AuctionRow> rows;

    public GetAuctionListResponse() { super(); }

    public GetAuctionListResponse(String message, String sessionId,
                                  boolean success, List<AuctionRow> rows) {
        super("AUCTION_LIST_RESULT", message, sessionId);
        this.success = success;
        this.rows    = rows;
    }

    public boolean isSuccess()        { return success; }
    public List<AuctionRow> getRows() { return rows; }

    public static class AuctionRow {
        public int    itemId;
        public String itemName;
        public String itemType;
        public BigDecimal currentPrice;
        public String status;
        public int    auctionId;
        public int    sellerId;

        public AuctionRow() {}
        public AuctionRow(int itemId, String itemName, String itemType,
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
}
