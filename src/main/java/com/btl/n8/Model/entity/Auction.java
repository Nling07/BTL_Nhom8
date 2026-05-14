package com.btl.n8.Model.entity;
import com.btl.n8.Model.enums.AuctionStatus;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class Auction extends Entity {
    private int itemId;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    public Auction(){
        super();
    }
    public Auction(int id, int itemId, BigDecimal startingPrice,
                   BigDecimal currentPrice, LocalDateTime startTime,
                   LocalDateTime endTime, AuctionStatus status) {
        super(id);
        this.itemId = itemId;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }


    // Setter
    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    // Getter
    public int getItemId() {
        return itemId;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }
}
