package com.btl.n8.Controller.dto;

public class BidRequest extends Request {
    private int auctionID;
    private double price;
    private String type;
    public BidRequest(){}
    public BidRequest(int auctionID,double price,String type){
        super("BID");
        this.price = price;
        this.type = type;
    }
    public double getPrice(){
        return price;
    }
    public void setPrice(double newprice){
        this.price= newprice;
    }
    public String getType() {return type;}
    public void setType(String newtype) {this.type = newtype;}
}
