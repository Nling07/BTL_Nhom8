package com.btl.n8.dto;
//Request lại từ server cho
public class Response {
    private String action;
    private String message; //phần thông báo cho user
    private Object data; //để object để còn trả về bất kì kiểu data

    public Response() {}

    public Response(String action, String message, Object data) {
        this.action = action;
        this.message = message;
        this.data = data;
    }

    public String getType() { return action; }
    public void setType(String newaction) { this.action = newaction; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
