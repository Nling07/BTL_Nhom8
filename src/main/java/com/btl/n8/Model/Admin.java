package com.btl.n8.Model;

public class Admin extends User{
    public Admin(String account, String password) {
        super(account, password, Role.ADMIN);
    }
    public Admin(int id, String account, String password) {
        super(id, account, password, Role.ADMIN);
    }
}
