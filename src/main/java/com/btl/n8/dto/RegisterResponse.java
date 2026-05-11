package com.btl.n8.DTO;

public class RegisterResponse extends Response {
    private int userId;
    private String username;
    private boolean success;
    private String sessionId;
    public RegisterResponse() {
        super();
    }

    public RegisterResponse(String message,String sessionId,boolean success, int userId, String username) {
        super("REGISTER_SUCCESS",message,sessionId);
        this.userId = userId;
        this.username = username;
        this.success = success;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public boolean isSuccess() { return success; }

}
