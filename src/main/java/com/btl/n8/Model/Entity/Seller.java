package com.btl.n8.Model.Entity;
import com.btl.n8.Model.Enums.Role;

public class Seller extends User {
    public Seller(){}

    public Seller(String account, String password) {
        super(account, password, Role.SELLER);
    }
    public Seller(int id, String account, String password) {
        super(id, account, password, Role.SELLER);
    }


}
