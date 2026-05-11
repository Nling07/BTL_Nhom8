package com.btl.n8.Model;

import java.math.BigDecimal;

public abstract class User {
    protected int id;
    protected String account;
    protected String password;
    protected Role role;
    protected BigDecimal balance;

    public User(){}
    public User(int id, String account, String password, Role role){
        this.id = id;
        this.account = account;
        this.password = password;
        this.role = role;
    }
    //thêm constructor để có balance.
    public User(int id, String account, String password, Role role,BigDecimal balance){
        this.id = id;
        this.account = account;
        this.password = password;
        this.role = role;
        this.balance = balance;
    }

    public User(String account, String password, Role role){
        this.account = account;
        this.password = password;
        this.role = role;
    }

    // Setter
    public void setId(int id){
        this.id = id;
    }
    public void setAccount(String account){
        this.account = account;
    }
    public void setPassword(String password){
        this.password = password;
    }
    public void setRole(Role role){
        this.role = role;
    }


    // Getter
    public int getId(){
        return id;
    }
    public String getAccount(){
        return account;
    }
    public String getPassword(){
        return password;
    }
    public Role getRole(){
        return role;
    }
    public BigDecimal getBalance(){return balance;}

}
