package com.btl.n8.Controller;

import com.btl.n8.Model.User;

public class SessionManager {
    //phần này bạn định thêm token vào để ở server thì verify user.
    private static volatile SessionManager instance;
    private volatile User currentUser;
    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        this.currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}