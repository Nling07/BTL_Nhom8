package com.btl.n8.DTO;

public class UpgradeSellerRequest extends Request {
    private int userId;

    public UpgradeSellerRequest() {}

    public UpgradeSellerRequest(String sessionId, int userId) {
        super("UPGRADE_SELLER", sessionId);
        this.userId = userId;
    }

    public int getUserId() { return userId; }
}
