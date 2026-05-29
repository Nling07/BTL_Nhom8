package com.btl.n8.DTO;

import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Bid;

import java.util.List;

public class GetAuctionDetailResponse extends Response {
    private boolean      success;
    private Auction      auction;
    private List<Bid>    bidHistory;

    public GetAuctionDetailResponse() { super(); }

    public GetAuctionDetailResponse(String message, String sessionId,
                                    boolean success,
                                    Auction auction,
                                    List<Bid> bidHistory) {
        super("AUCTION_DETAIL_RESULT", message, sessionId);
        this.success    = success;
        this.auction    = auction;
        this.bidHistory = bidHistory;
    }

    public boolean   isSuccess()        { return success; }
    public Auction   getAuction()       { return auction; }
    public List<Bid> getBidHistory()    { return bidHistory; }
}
