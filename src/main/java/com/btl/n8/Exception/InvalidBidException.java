package com.btl.n8.Exception;

/**
 * Ném ra khi giá đặt không hợp lệ.
 * Ví dụ: giá thấp hơn hoặc bằng giá hiện tại, giá âm, hoặc null.
 */
public class InvalidBidException extends RuntimeException {

    private final double currentPrice;
    private final double attemptedPrice;

    public InvalidBidException(String message) {
        super(message);
        this.currentPrice   = -1;
        this.attemptedPrice = -1;
    }

    public InvalidBidException(String message, double currentPrice, double attemptedPrice) {
        super(message);
        this.currentPrice   = currentPrice;
        this.attemptedPrice = attemptedPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public double getAttemptedPrice() {
        return attemptedPrice;
    }
}