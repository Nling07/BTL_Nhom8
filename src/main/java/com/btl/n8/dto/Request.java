package com.btl.n8.DTO;

// lấy request từ client sang server
public class Request {
    private String action;
    public Request(){}
    public Request(String action){
        this.action = action;
    }

    public String getAction(){return action;}

}
