package com.btl.n8.DTO;

import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Item;
import java.util.List;

public class GetSellerItemsResponse extends Response {
    private boolean      success;
    private List<Item>   items;
    private List<Auction> auctions;

    public GetSellerItemsResponse() { super(); }

    public GetSellerItemsResponse(String message, String sessionId,
                                  boolean success,
                                  List<Item> items,
                                  List<Auction> auctions) {
        super("SELLER_ITEMS_RESULT", message, sessionId);
        this.success  = success;
        this.items    = items;
        this.auctions = auctions;
    }

    public boolean       isSuccess()  { return success; }
    public List<Item>    getItems()   { return items; }
    public List<Auction> getAuctions(){ return auctions; }
}
