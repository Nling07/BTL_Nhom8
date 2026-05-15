package com.btl.n8.dto;

import java.math.BigDecimal;

public class AutoBidResponse extends Response {

    private boolean    success;
    private int        auctionId;
    private BigDecimal maxPrice;
    private BigDecimal step;
    private boolean    snipeMode;
    private int        snipeSeconds;
    private boolean    active;   // autobid có đang chạy không

    public AutoBidResponse() { super(); }

    public AutoBidResponse(String message, String sessionId, boolean success,
                           int auctionId, BigDecimal maxPrice, BigDecimal step,
                           boolean snipeMode, int snipeSeconds, boolean active) {
        super("AUTO_BID_RESPONSE", message, sessionId);
        this.success      = success;
        this.auctionId    = auctionId;
        this.maxPrice     = maxPrice;
        this.step         = step;
        this.snipeMode    = snipeMode;
        this.snipeSeconds = snipeSeconds;
        this.active       = active;
    }

    public boolean    isSuccess()       { return success; }
    public int        getAuctionId()    { return auctionId; }
    public BigDecimal getMaxPrice()     { return maxPrice; }
    public BigDecimal getStep()         { return step; }
    public boolean    isSnipeMode()     { return snipeMode; }
    public int        getSnipeSeconds() { return snipeSeconds; }
    public boolean    isActive()        { return active; }
}
