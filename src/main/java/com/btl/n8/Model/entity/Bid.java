package com.btl.n8.Model.entity;
import com.btl.n8.Model.enums.BidStatus;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class Bid extends Entity {
    private int auctionId;
    private int bidderId;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private BidStatus status;

    public Bid() {
        super();
    }

    public Bid(int id, int auctionId, int bidderId, BigDecimal amount, LocalDateTime bidTime, BidStatus status) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.bidTime = bidTime;
        this.status = status;
    }

    // Setter
    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }
    public void setBidderId(int bidderId) {
        this.bidderId = bidderId;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
    public void setStatus(BidStatus status) {
        this.status = status;
    }

    // Getter
    public int getAuctionId() {
        return auctionId;
    }
    public int getBidderId() {
        return bidderId;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public LocalDateTime getBidTime() {
        return bidTime;
    }
    public BidStatus getStatus() {
        return status;
    }
}
