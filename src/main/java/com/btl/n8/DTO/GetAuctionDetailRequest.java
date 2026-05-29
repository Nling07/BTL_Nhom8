package com.btl.n8.DTO;

public class GetAuctionDetailRequest extends Request {
    private int auctionId;

    public GetAuctionDetailRequest() {}
    public GetAuctionDetailRequest(String sessionId, int auctionId) {
        super("GET_AUCTION_DETAIL", sessionId);
        this.auctionId = auctionId;
    }

    public int getAuctionId() { return auctionId; }
}
