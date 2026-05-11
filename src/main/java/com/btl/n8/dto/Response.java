package com.btl.n8.DTO;
//Request lại từ server cho
public abstract class Response {
    private String action;
    private String message; //phần thông báo cho user

    public Response() {}

    public Response(String action) {
        this.action = action;

    }
    public String getAction() { return action; }

}
