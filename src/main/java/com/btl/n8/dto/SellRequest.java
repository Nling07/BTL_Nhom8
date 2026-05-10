package com.btl.n8.dto;

import java.math.BigDecimal;

public class SellRequest extends Request {

    private String itemName;
    private BigDecimal startPrice;
    private String Itemtype;
    public SellRequest(){}
    public SellRequest( String itemName, BigDecimal startPrice, String Itemtype) {
        super("SELL");
        this.itemName = itemName;
        this.startPrice = startPrice;
        this.Itemtype = Itemtype;
    }
    public BigDecimal getStartPrice(){return startPrice;}
    public void setStartPrice(String newtype){this.Itemtype = newtype;}
    public String getItemName(){return itemName;}
    public void setItemName(String newitemName){this.itemName = newitemName;}
}
