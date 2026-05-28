package com.btl.n8.DTO;

public class GetAuctionDetailRequest extends Request {
    private int auctionId;
    public GetAuctionDetailRequest(int auctionId) {
        super("GET_AUCTION_DETAIL");
        this.auctionId = auctionId;
    }
    public int getAuctionId() { return auctionId; }
}
