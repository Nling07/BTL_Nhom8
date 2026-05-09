package com.btl.n8.Service;

import com.btl.n8.Connection.UserDAO;
import com.btl.n8.Model.User;
import com.btl.n8.Model.Role;

public class UserService {
    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Đăng ký user mới
    public boolean register(User user) {
        if (user.getAccount() == null || user.getAccount().isBlank()) {
            return false; // kiểm tra account hợp lệ
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return false; // kiểm tra password hợp lệ
        }
        return userDAO.insert(user);
    }

    // Đăng nhập
    public User login(String account, String password) {
        User user = userDAO.findByAccount(account);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    // Cập nhật thông tin user
    public boolean updateUser(User user) {
        return userDAO.update(user);
    }

    // Lấy user theo id
    public User getUserById(int id) {
        return userDAO.findById(id);
    }

    // Lấy user theo account
    public User getUserByAccount(String account) {
        return userDAO.findByAccount(account);
    }

    // Kiểm tra role của user
    public boolean isRole(User user, Role role) {
        return user != null && user.getRole() == role;
    }
}
