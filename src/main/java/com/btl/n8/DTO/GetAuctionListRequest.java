package com.btl.n8.DTO;

public class GetAuctionListRequest extends Request {
    public GetAuctionListRequest() {}
    public GetAuctionListRequest(String sessionId) {
        super("GET_AUCTION_LIST", sessionId);
    }
}
