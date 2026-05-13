package com.btl.n8.dto;

import java.time.LocalDateTime;

public class AddItemResponse extends Response {
    private boolean success;
    private int itemId;
    private int auctionId;
    private LocalDateTime newtime;

    public AddItemResponse() { super(); }

    public AddItemResponse(String message, String sessionId, boolean success,
                           int auctionId, int itemId, LocalDateTime newtime) {
        super("ADD_ITEM_SUCCESS", message, sessionId);
        this.success   = success;
        this.auctionId = auctionId;
        this.itemId    = itemId;
        this.newtime   = newtime;
    }

    public boolean isSuccess()         { return success; }
    public int getItemId()             { return itemId; }
    public int getAuctionId()          { return auctionId; }
    public LocalDateTime getNewtime()  { return newtime; }
}