package com.btl.n8.DTO;

import com.btl.n8.Model.ItemType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AddItemRequest extends Request {

    private String name;
    private ItemType type;
    private BigDecimal startingPrice;
    private String imageBase64;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int sellerId;

    public AddItemRequest() {}

    public AddItemRequest(String name, String sessionId,ItemType type, BigDecimal startingPrice,
                          String imageBase64, LocalDateTime startTime,
                          LocalDateTime endTime, int sellerId) {
        super("ADD_ITEM",sessionId);
        this.name = name;
        this.type = type;
        this.startingPrice = startingPrice;
        this.imageBase64 = imageBase64;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }

    public String getName()             { return name; }
    public ItemType getType()           { return type; }        // trả về ItemType
    public BigDecimal getStartingPrice(){ return startingPrice; }
    public String getImageBase64()      { return imageBase64; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime()   { return endTime; }
    public int getSellerId()            { return sellerId; }
}