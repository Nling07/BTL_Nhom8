package com.btl.n8.Network;

import com.btl.n8.Model.Entity.User;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerSessionManager {
    private static ServerSessionManager instance;

    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    private ServerSessionManager() {}

    public static synchronized ServerSessionManager getInstance() {
        if (instance == null) {
            instance = new ServerSessionManager();
        }
        return instance;
    }

    public String createSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user);
        return sessionId;
    }

    public User getUser(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * FIX: cập nhật user object trong session với dữ liệu mới nhất từ DB.
     * Gọi sau khi balance thay đổi (settlement, nạp tiền, v.v.) để
     * balance check trong handleBid() luôn dùng giá trị đúng.
     */
    public void refreshUser(String sessionId, User freshUser) {
        if (sessionId != null && freshUser != null) {
            sessions.put(sessionId, freshUser);
        }
    }

    public boolean isValid(String token) {
        return sessions.containsKey(token);
    }

    public void removeSession(String token) {
        sessions.remove(token);
    }

    public void updateUser(String sessionId, User updatedUser) {
        sessions.put(sessionId, updatedUser);
    }
}