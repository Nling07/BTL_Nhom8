package com.btl.n8.DTO;

public class RegisterRequest extends Request{
    private String userName;
    private String userPassword;

    public RegisterRequest(){}
    public RegisterRequest(String userName, String userPassword,String userRePassword){
        super("REGISTER");
        this.userName = userName;
        this.userPassword = userPassword;
    }
    public String getUserName(){return userName;}

    public String getUserPassword(){return userPassword;}
}
