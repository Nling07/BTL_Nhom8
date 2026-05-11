package com.btl.n8.DTO;

import com.btl.n8.Controller.SessionManager;
import com.btl.n8.Network.ServerResponseListener;
import com.btl.n8.Network.ServerSessionManager;

//Request lại từ server cho
public abstract class Response {
    private String action;
    private String message;//phần thông báo cho user.
    private String sessionId;

    public Response() {}

    public Response(String action,String message,String sessionId) {
        this.action = action;
        this.message = message;
        this.sessionId= sessionId;

    }
    public String getAction() { return action; }
    public String getMessage(){return message;}
    public String getSessionId(){return  sessionId;}

}
