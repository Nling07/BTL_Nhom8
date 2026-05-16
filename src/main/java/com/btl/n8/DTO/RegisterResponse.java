package com.btl.n8.DTO;

public class RegisterResponse extends Response {
    private boolean success;
    private int userId;
    private String username;

    public RegisterResponse() { super(); }

    public RegisterResponse(String message, String sessionId, boolean success,
                            int userId, String username) {
        super("REGISTER_SUCCESS", message, sessionId);
        this.success  = success;
        this.userId   = userId;
        this.username = username;
    }

    public boolean isSuccess()     { return success; }
    public int getUserId()         { return userId; }
    public String getUsername()    { return username; }
}