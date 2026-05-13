package com.btl.n8.dto;

public class Request {
    private String action;
    private String sessionId;

    public Request() {}

    public Request(String action) {
        this.action = action;
    }

    public Request(String action, String sessionId) {
        this.action    = action;
        this.sessionId = sessionId;
    }

    public String getAction()                      { return action; }
    public String getSessionId()                   { return sessionId; }
    public void setSessionId(String sessionId)     { this.sessionId = sessionId; }
}