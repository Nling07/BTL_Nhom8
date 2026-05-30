package com.btl.n8.DTO;

public class GetUserBalanceRequest extends Request {

    private int userId;

    public GetUserBalanceRequest() { super(); }

    public GetUserBalanceRequest(String sessionId, int userId) {
        super("GET_USER_BALANCE", sessionId);
        this.userId = userId;
    }

    public int getUserId() { return userId; }
}