package com.btl.n8.dto;

import com.btl.n8.Model.enums.Role;
import java.math.BigDecimal;

public class LoginResponse extends Response {
    private boolean success;
    private int userId;
    private String username;
    private Role role;
    private BigDecimal balance;

    public LoginResponse() { super(); }

    public LoginResponse(String message, String sessionId, boolean success,
                         int userId, String username, Role role, BigDecimal balance) {
        super("LOGIN_SUCCESS", message, sessionId);
        this.success  = success;
        this.userId   = userId;
        this.username = username;
        this.role     = role;
        this.balance  = balance;
    }

    public boolean isSuccess()         { return success; }
    public int getUserId()             { return userId; }
    public String getUsername()        { return username; }
    public Role getRole()              { return role; }
    public BigDecimal getBalance()     { return balance; }
}