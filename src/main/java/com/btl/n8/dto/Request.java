package com.btl.n8.dto;

import java.util.UUID;

// lấy request từ client sang server
public abstract class Request {
    private String action;
    private String sessionID;

    public Request(){}

    public Request(String action){
        this.action = action;
        this.sessionID = UUID.randomUUID().toString();
    }

    public String getType(){
        return action;
    }

    public void setType(String newtype){
        this.action = newtype;
    }
    public String getSessionID(){
        return sessionID;
    }
    public void setSessionID(String newsessionID){
        this.sessionID = newsessionID;
    }

}
