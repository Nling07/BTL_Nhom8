package com.btl.n8.DTO;

import com.btl.n8.Model.Enums.BidStatus;

import java.math.BigDecimal;

public class BidRequest extends Request {

    private int auctionId;
    private int bidderId;
    private BigDecimal amount;
    private BidStatus status;

    public BidRequest() {}

    public BidRequest(
            int auctionId,
            int bidderId,
            BigDecimal amount
    ) {

        super("BID");

        this.auctionId = auctionId;
        this.bidderId  = bidderId;
        this.amount    = amount;
        this.status    = BidStatus.ACTIVE;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public int getBidderId() {
        return bidderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BidStatus getStatus() {
        return status;
    }
}