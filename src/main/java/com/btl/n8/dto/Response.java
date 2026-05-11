package com.btl.n8.DTO;
//Request lại từ server cho
public class Response {
    private String action;
    private String message; //phần thông báo cho user

    public Response() {}

    public Response(String action, String message) {
        this.action = action;
        this.message = message;
    }

    public String getAction() { return action; }

    public String getMessage() { return message; }
}
