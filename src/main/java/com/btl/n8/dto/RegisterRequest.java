package com.btl.n8.dto;

public class RegisterRequest extends Request {
    private String userName;
    private String userPassword;

    public RegisterRequest() {}

    public RegisterRequest(String userName, String userPassword) {
        super("REGISTER");
        this.userName     = userName;
        this.userPassword = userPassword;
    }

    public String getUserName()     { return userName; }
    public String getUserPassword() { return userPassword; }
}