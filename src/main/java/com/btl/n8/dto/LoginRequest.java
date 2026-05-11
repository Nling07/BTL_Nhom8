package com.btl.n8.DTO;

public class LoginRequest extends Request{
    private String username;
    private String password;
    public LoginRequest(String username, String password){}
    public LoginRequest(String username, String password,String sessionId){
        super("LOGIN");
        this.username=username;
        this.password=password;
    }
    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }
}
