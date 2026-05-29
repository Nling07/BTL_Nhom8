package com.btl.n8.DTO;

import com.btl.n8.Model.Entity.User;

public class UpgradeSellerResponse extends Response {
    private boolean success;
    private User updatedUser;

    public UpgradeSellerResponse() { super(); }

    public UpgradeSellerResponse(String message, String sessionId,
                                 boolean success, User updatedUser) {
        super("UPGRADE_SELLER_RESULT", message, sessionId);
        this.success     = success;
        this.updatedUser = updatedUser;
    }

    public boolean isSuccess()     { return success; }
    public User getUpdatedUser()   { return updatedUser; }
}
