package com.btl.n8.Model.Entity;

import com.btl.n8.Model.Enums.Role;

import java.math.BigDecimal;

public abstract class User extends Entity {
    protected String account;
    protected String password;
    protected Role role;
    protected BigDecimal balance;
    protected BigDecimal frozenBalance; // tiền đang bị khóa do đang đấu giá

    public User(){
        super();
        this.frozenBalance = BigDecimal.ZERO;
    }

    public User(int id, String account, String password, Role role){
        super(id);
        this.account = account;
        this.password = password;
        this.role = role;
        this.frozenBalance = BigDecimal.ZERO;
    }

    public User(int id, String account, String password, Role role, BigDecimal balance){
        super(id);
        this.account = account;
        this.password = password;
        this.role = role;
        this.balance = balance;
        this.frozenBalance = BigDecimal.ZERO;
    }

    public User(int id, String account, String password, Role role, BigDecimal balance, BigDecimal frozenBalance){
        super(id);
        this.account = account;
        this.password = password;
        this.role = role;
        this.balance = balance;
        this.frozenBalance = frozenBalance != null ? frozenBalance : BigDecimal.ZERO;
    }

    public User(String account, String password, Role role){
        this.account = account;
        this.password = password;
        this.role = role;
        this.frozenBalance = BigDecimal.ZERO;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getAccount()             { return account; }
    public String getPassword()            { return password; }
    public Role getRole()                  { return role; }
    public BigDecimal getBalance()         { return balance; }
    public BigDecimal getFrozenBalance()   { return frozenBalance != null ? frozenBalance : BigDecimal.ZERO; }

    /**
     * Số dư thực sự có thể dùng = balance - frozenBalance.
     * Dùng cái này để check khi đặt giá mới.
     */
    public BigDecimal getAvailableBalance() {
        BigDecimal bal    = balance != null ? balance : BigDecimal.ZERO;
        BigDecimal frozen = frozenBalance != null ? frozenBalance : BigDecimal.ZERO;
        return bal.subtract(frozen).max(BigDecimal.ZERO);
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setAccount(String account)               { this.account = account; }
    public void setPassword(String password)             { this.password = password; }
    public void setRole(Role role)                       { this.role = role; }
    public void setBalance(BigDecimal balance)           { this.balance = balance; }
    public void setFrozenBalance(BigDecimal frozen)      { this.frozenBalance = frozen != null ? frozen : BigDecimal.ZERO; }
}