package com.btl.n8.DTO;

public class RegisterRequest extends Request {
    private String userName;
    private String passWord;

    public RegisterRequest() {}

    public RegisterRequest(String userName, String passWord) {
        super("REGISTER");
        this.userName = userName;
        this.passWord = passWord;
    }

    public String getUserName()     { return userName; }
    public String getUserPassword() { return passWord; }
}