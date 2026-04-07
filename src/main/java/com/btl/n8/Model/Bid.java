package com.btl.n8.Model;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class Bid {
    private int id;
    private int auctionId;
    private int bidderId;
    private BigDecimal amount;
    private LocalDateTime bidTime;

    public Bid() {}

    public Bid(int id, int auctionId, int bidderId, BigDecimal amount, LocalDateTime bidTime) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.bidTime = bidTime;
    }

    // Getter - Setter - tối làm
    // thêm constructor rỗng cho linh hoạt
    // viết nốt auction
}
