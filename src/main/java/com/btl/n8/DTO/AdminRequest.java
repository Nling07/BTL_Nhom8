package com.btl.n8.DTO;

public class AdminRequest extends Request {
    private int targetId;  // userId / auctionId / itemId tùy action

    public AdminRequest() {}
    public AdminRequest(String action, String sessionId, int targetId) {
        super(action, sessionId);
        this.targetId = targetId;
    }

    public int getTargetId() { return targetId; }
}
