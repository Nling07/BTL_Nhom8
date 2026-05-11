package com.btl.n8.Network;

import com.btl.n8.Model.User;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerSessionManager {
    private static ServerSessionManager instance;

    private final Map<String, User> sessions = new ConcurrentHashMap<>();
    private ServerSessionManager(){}
    public static synchronized ServerSessionManager getInstance(){
        if(instance == null){
            instance = new ServerSessionManager();
        }
        return instance;
    }
    public String createSession(User user){

        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user);
        return sessionId;
    }
    public User getUser(String sessionId){
        return sessions.get(sessionId);
    }
    public boolean isValid(String token){
        return sessions.containsKey(token);
    }

    public void removeSession(String token){
        sessions.remove(token);
    }
}
