package com.btl.n8.Model;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class Bid {
    private int id;
    private int auctionId;
    private int bidderId;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private BidStatus status;

    public Bid() {}

    public Bid(int id, int auctionId, int bidderId, BigDecimal amount, LocalDateTime bidTime, BidStatus status) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.bidTime = bidTime;
        this.status = status;
    }

    // Getter & Setter cho id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Getter & Setter cho auctionId
    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    // Getter & Setter cho bidderId
    public int getBidderId() {
        return bidderId;
    }

    public void setBidderId(int bidderId) {
        this.bidderId = bidderId;
    }

    // Getter & Setter cho amount
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    // Getter & Setter cho bidTime
    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    // Getter & Setter cho Status
    public BidStatus getStatus() {
        return status;
    }

    public void setStatus(BidStatus status) {
        this.status = status;
    }
}
