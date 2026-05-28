package com.btl.n8.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidResponse extends Response {
    private boolean       success;
    private int           auctionId;
    private BigDecimal    currentPrice;
    private LocalDateTime time;
    private int           bidderId;
    /**
     * Anti-sniping: nếu server gia hạn thời gian auction, field này chứa
     * endTime mới. null nếu không có gia hạn.
     */
    private LocalDateTime newEndTime;

    public BidResponse() { super(); }

    public BidResponse(String message, String sessionId, boolean success,
                       int auctionId, BigDecimal currentPrice,
                       LocalDateTime time, int bidderId) {
        super("BID_UPDATE", message, sessionId);
        this.success      = success;
        this.auctionId    = auctionId;
        this.currentPrice = currentPrice;
        this.time         = time;
        this.bidderId     = bidderId;
        this.newEndTime   = null;
    }

    public boolean isSuccess()              { return success; }
    public int getAuctionId()               { return auctionId; }
    public BigDecimal getCurrentPrice()     { return currentPrice; }
    public LocalDateTime getTime()          { return time; }
    public int getBidderId()                { return bidderId; }
    public LocalDateTime getNewEndTime()    { return newEndTime; }
    public void setNewEndTime(LocalDateTime newEndTime) { this.newEndTime = newEndTime; }
}