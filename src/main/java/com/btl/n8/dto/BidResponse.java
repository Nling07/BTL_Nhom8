package com.btl.n8.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidResponse extends Response {
    private boolean success;
    private int auctionId;
    private BigDecimal currentPrice;
    private LocalDateTime time;

    public BidResponse() { super(); }

    public BidResponse(String message, String sessionId, boolean success,
                       int auctionId, BigDecimal currentPrice, LocalDateTime time) {
        super("BID_UPDATE", message, sessionId);
        this.success      = success;
        this.auctionId    = auctionId;
        this.currentPrice = currentPrice;
        this.time         = time;
    }

    public boolean isSuccess()          { return success; }
    public int getAuctionId()           { return auctionId; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public LocalDateTime getTime()      { return time; }
}