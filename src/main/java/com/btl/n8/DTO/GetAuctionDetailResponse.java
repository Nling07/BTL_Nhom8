package com.btl.n8.DTO;

import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Bid;
import com.btl.n8.Model.Entity.Item;

import java.util.List;

public class GetAuctionDetailResponse extends Response {
    private Auction auction;
    private Item item;
    private List<Bid> bidHistory;

    public GetAuctionDetailResponse(String msg, String sid,
                                    Auction auction, Item item,
                                    List<Bid> bidHistory) {
        super("AUCTION_DETAIL", msg, sid);
        this.auction    = auction;
        this.item       = item;
        this.bidHistory = bidHistory;
    }
    public Auction   getAuction()    { return auction; }
    public Item      getItem()       { return item; }
    public List<Bid> getBidHistory() { return bidHistory; }
}
