package com.btl.n8.DTO;

import com.btl.n8.Controller.BidController;

import java.util.List;

public class GetAuctionListResponse extends Response {
    private List<BidController.ItemAuctionRow> rows;
    public GetAuctionListResponse(String msg, String sid, List<BidController.ItemAuctionRow> rows) {
        super("AUCTION_LIST", msg, sid);
        this.rows = rows;
    }
    public List<BidController.ItemAuctionRow> getRows() { return rows; }
}
