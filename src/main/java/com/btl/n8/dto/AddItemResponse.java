package com.btl.n8.DTO;

import java.time.LocalDateTime;

public class AddItemResponse extends Response {
    private int itemId;
    private boolean success;
    private LocalDateTime newtime;
    private int auctionId;
    private String message;

    public AddItemResponse() {
        super();
    }

    public AddItemResponse(String message, String sessionId,boolean success, int auctionId,int itemId,LocalDateTime newtime) {
        super("ADD_ITEM",message,sessionId);
        this.message = message;
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.success = success;
        this.newtime = newtime;
    }

    public int getItemId() { return itemId; }
    public int getAuctionId(){return auctionId;}
    public boolean isSuccess() { return success; }
    public LocalDateTime getNewtime(){return newtime;}

}
