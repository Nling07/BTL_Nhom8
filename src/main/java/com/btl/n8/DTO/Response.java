package com.btl.n8.DTO;

public abstract class Response {
    private String action;
    private String message;
    private String sessionId;

    public Response() {}

    public Response(String action, String message, String sessionId) {
        this.action    = action;
        this.message   = message;
        this.sessionId = sessionId;
    }

    public String getAction()    { return action; }
    public String getMessage()   { return message; }
    public String getSessionId() { return sessionId; }
}