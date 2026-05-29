package com.btl.n8.DTO;

public class GetUserBidsRequest extends Request {
    private int userId;

    public GetUserBidsRequest() {}

    public GetUserBidsRequest(String sessionId, int userId) {
        super("GET_USER_BIDS", sessionId);
        this.userId = userId;
    }

    public int getUserId() { return userId; }
}
