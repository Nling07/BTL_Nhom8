package com.btl.n8.Model;
import java.util.ArrayList;
public class Seller extends User{
    private ArrayList<Integer> IdItem;
    public Seller(String account, String password) {
        super(account, password);
        this.setRole(Role.SELLER);
    }
    public Seller(int id, String account, String password) {
        super(id, account, password);
        this.setRole(Role.SELLER);
    }

    public void addItem(Item item){

        IdItem.add(item.getId());
    }
    public void removeItem(Item item){

        IdItem.remove(item.getId());
    }
    public ArrayList<Integer> getIdItem(){

        return IdItem;
    }
}
