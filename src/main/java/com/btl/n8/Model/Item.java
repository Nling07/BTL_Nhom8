package com.btl.n8.Model;

public abstract class Item {
    protected int id;
    protected String name;
    //protected Photo ?
    protected double startingPrice;
    protected int idSeller;
    public Item(int id, String name, double startingPrice, int idSeller){
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
        this.idSeller = idSeller;
    }

    // Setter
    public void setId(int id){
        this.id = id;
    }
    public void setname(String name){
        this.name = name;
    }
    public void setStartingPrice(double startingPrice){
        this.startingPrice = startingPrice;
    }

    // Getter
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public double getStartingPrice() {
        return startingPrice;
    }
    public int getIdSeller() {
        return idSeller;
    }
}
