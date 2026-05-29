package com.btl.n8.DTO;

public class LoginRequest extends Request {
    private String userName;
    private String passWord;

    public LoginRequest() {}

    public LoginRequest(String userName, String passWord) {
        super("LOGIN");
        this.userName = userName;
        this.passWord = passWord;
    }

    public String getUsername() { return userName; }
    public String getPassword() { return passWord; }
}