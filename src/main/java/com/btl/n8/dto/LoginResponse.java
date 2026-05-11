package com.btl.n8.DTO;

import com.btl.n8.Model.Role;

import java.math.BigDecimal;

public class LoginResponse extends Response {
    private String message;
    private  BigDecimal balance;
    private Role role;
    private int userId;
    private String username;
    private String token;
    private boolean success;

    public LoginResponse() {
        super();
    }

    public LoginResponse(String message, String sessionId,boolean success, int userId, String username, Role role, BigDecimal balance) {
        super("LOGIN_SUCCESS",message,sessionId);
        this.message=message;
        this.userId = userId;
        this.username = username;
        this.success = success;
        this.role = role;
        this.balance = balance;
    }


    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public boolean isSuccess() { return success; }
    public BigDecimal getBalance(){return balance;}
    public Role getRole(){return role;}

}
