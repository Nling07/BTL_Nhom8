package com.btl.n8.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidResponse extends Response {
    private int auctionId;
    private BigDecimal currentPrice;
    private boolean success;
    private LocalDateTime time;

    public BidResponse() {
        super();
    }

    public BidResponse(String message, String sessionId,boolean success, int auctionId, BigDecimal currentPrice,LocalDateTime time) {
        super("BID_UPDATE",message,sessionId);
        this.time = time;
        this.auctionId = auctionId;
        this.currentPrice = currentPrice;
        this.success = success;
    }


    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public boolean isSuccess() { return success; }
    public LocalDateTime time (){return time;}
}
