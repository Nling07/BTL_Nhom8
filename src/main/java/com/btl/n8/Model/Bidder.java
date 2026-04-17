package com.btl.n8.Model;
import java.math.BigDecimal;
public class Bidder extends User {
    private BigDecimal balance;

    public Bidder(){}

    public Bidder(String account, String password, BigDecimal balance) {
        super(account, password, Role.BIDDER);
        this.balance = balance;
    }

    public Bidder(int id, String account, String password, BigDecimal balance) {
        super(id, account, password, Role.BIDDER);
        this.balance = balance;
    }

    public void setBalance(BigDecimal balance){
        this.balance = balance;
    }

    public BigDecimal getBalance(){
        return balance;
    }
}
