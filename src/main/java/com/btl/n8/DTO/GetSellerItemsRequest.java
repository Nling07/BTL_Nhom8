package com.btl.n8.DTO;

public class GetSellerItemsRequest extends Request {
    private int sellerId;

    public GetSellerItemsRequest() {}

    public GetSellerItemsRequest(String sessionId, int sellerId) {
        super("GET_SELLER_ITEMS", sessionId);
        this.sellerId = sellerId;
    }

    public int getSellerId() { return sellerId; }
}
