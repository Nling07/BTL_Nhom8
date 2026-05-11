package com.btl.n8.DTO;

public class AddItemResponse extends Response {
    private int itemId;
    private boolean success;

    public AddItemResponse() {
        super();
    }

    public AddItemResponse(String message, boolean success, int itemId) {
        super("ADD_ITEM");
        this.itemId = itemId;
        this.success = success;
    }

    public int getItemId() { return itemId; }

    public boolean isSuccess() { return success; }

}
