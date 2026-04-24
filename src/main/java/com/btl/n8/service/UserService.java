package com.btl.n8.service;

import com.btl.n8.Connection.UserDAO;
import com.btl.n8.Model.User;

public class UserService {
    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }


    public boolean register(User user) {

        User existing = userDAO.findByAccount(user.getAccount());
        if (existing != null) {
            return false;
        }
        return userDAO.insert(user);
    }

    // Đăng nhập
    public boolean login(String account, String password) {
        User user = userDAO.findByAccount(account);
        if (user == null) return false;
        return user.getPassword().equals(password);
    }

    // Cập nhật thông tin người dùng
    public boolean updateUser(User user) {
        return userDAO.update(user);
    }

    // Lấy thông tin người dùng theo id
    public User getUserById(int id) {
        return userDAO.findById(id);
    }

    // Lấy thông tin người dùng theo account
    public User getUserByAccount(String account) {
        return userDAO.findByAccount(account);
    }
}
