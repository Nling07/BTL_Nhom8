package com.btl.n8.dto;

public class LoginRequest extends com.btl.n8.dto.Request {
    private String username;
    private String password;

    public LoginRequest() {}

    public LoginRequest(String username, String password) {
        super("LOGIN");
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}