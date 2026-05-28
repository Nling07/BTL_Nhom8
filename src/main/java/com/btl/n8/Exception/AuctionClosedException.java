package com.btl.n8.Exception;

/**
 * Ném ra khi người dùng cố đặt giá vào một phiên đấu giá đã đóng hoặc đã hết giờ.
 */
public class AuctionClosedException extends RuntimeException {

    private final int auctionId;

    public AuctionClosedException(String message) {
        super(message);
        this.auctionId = -1;
    }

    public AuctionClosedException(String message, int auctionId) {
        super(message);
        this.auctionId = auctionId;
    }

    public int getAuctionId() {
        return auctionId;
    }
}