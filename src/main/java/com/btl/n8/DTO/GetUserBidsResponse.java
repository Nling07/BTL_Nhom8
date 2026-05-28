package com.btl.n8.DTO;

import com.btl.n8.Model.Entity.Bid;
import java.util.List;

public class GetUserBidsResponse extends Response {
    private boolean success;
    private List<Bid> bids;

    public GetUserBidsResponse() { super(); }

    public GetUserBidsResponse(String message, String sessionId,
                               boolean success, List<Bid> bids) {
        super("USER_BIDS_RESULT", message, sessionId);
        this.success = success;
        this.bids    = bids;
    }

    public boolean isSuccess()  { return success; }
    public List<Bid> getBids()  { return bids; }
}
