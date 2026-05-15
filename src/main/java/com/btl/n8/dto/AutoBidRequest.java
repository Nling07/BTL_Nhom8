package com.btl.n8.dto;

import java.math.BigDecimal;

public class AutoBidRequest extends Request {

    private int        auctionId;
    private int        bidderId;
    private BigDecimal maxPrice;
    private BigDecimal step;
    private boolean    snipeMode;   // true = chỉ bid trong N giây cuối
    private int        snipeSeconds; // số giây trước endTime để kích hoạt snipe

    public AutoBidRequest() {}

    public AutoBidRequest(int auctionId, int bidderId,
                          BigDecimal maxPrice, BigDecimal step,
                          boolean snipeMode, int snipeSeconds) {
        super("AUTO_BID");
        this.auctionId    = auctionId;
        this.bidderId     = bidderId;
        this.maxPrice     = maxPrice;
        this.step         = step;
        this.snipeMode    = snipeMode;
        this.snipeSeconds = snipeSeconds;
    }

    public int        getAuctionId()    { return auctionId; }
    public int        getBidderId()     { return bidderId; }
    public BigDecimal getMaxPrice()     { return maxPrice; }
    public BigDecimal getStep()         { return step; }
    public boolean    isSnipeMode()     { return snipeMode; }
    public int        getSnipeSeconds() { return snipeSeconds; }
}
