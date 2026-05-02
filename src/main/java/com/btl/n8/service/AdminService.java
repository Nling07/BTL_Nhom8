package com.btl.n8.service;

import com.btl.n8.Connection.UserDAO;
import com.btl.n8.Model.Admin;
import com.btl.n8.Model.Bidder;
import com.btl.n8.Model.User;
import com.btl.n8.Model.Role;

public class AdminService {
    private final UserDAO userDAO;

    public AdminService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Đăng ký admin mới
    public boolean registerAdmin(Admin admin) {
        User existing = userDAO.findByAccount(admin.getAccount());
        if (existing != null) {
            return false; // account đã tồn tại
        }
        return userDAO.insert(admin); // đa hình: Admin là User
    }

    // Đăng nhập
    public boolean login(String account, String password) {
        User user = userDAO.findByAccount(account);
        if (user != null && user.getRole() == Role.ADMIN) {
            return user.getPassword().equals(password);
        }
        return false;
    }

    // Lấy thông tin admin theo id
    public Admin getAdminById(int id) {
        User user = userDAO.findById(id);
        if (user instanceof Admin admin && user.getRole() == Role.ADMIN) {
            return admin;
        }
        return null;
    }
    public Admin getAdminByAccount(String account) {
        User user = userDAO.findByAccount(account);
        if (user instanceof Admin admin && user.getRole() == Role.ADMIN) {
            return admin;
        }
        return null;
    }

    // Cập nhật thông tin admin
    public boolean updateAdmin(Admin admin) {
        return userDAO.update(admin); // đa hình: Admin là User
    }
}
