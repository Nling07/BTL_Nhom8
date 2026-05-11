package com.btl.n8.DTO;

import java.math.BigDecimal;

public class BidResponse extends Response {
    private int auctionId;
    private BigDecimal currentPrice;
    private boolean success;

    public BidResponse() {
        super();
    }

    public BidResponse(String message, boolean success, int auctionId, BigDecimal currentPrice) {
        super("BID_UPDATE");
        this.auctionId = auctionId;
        this.currentPrice = currentPrice;
        this.success = success;
    }


    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public boolean isSuccess() { return success; }

}
