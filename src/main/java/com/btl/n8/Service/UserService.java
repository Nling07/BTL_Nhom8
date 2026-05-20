package com.btl.n8.Service;

import com.btl.n8.Connection.UserDAO;
import com.btl.n8.Model.Entity.Admin;
import com.btl.n8.Model.Entity.Bidder;
import com.btl.n8.Model.Entity.User;
import com.btl.n8.Model.Enums.Role;

import java.math.BigDecimal;

public class UserService {
    private final UserDAO userDAO;

    // ── Tài khoản admin cố định ───────────────────────────────────────────────
    // Không cần row trong DB. Đổi username/password ở đây nếu muốn.
    private static final String ADMIN_ACCOUNT  = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final int    ADMIN_ID       = 0;      // id giả, không dùng DB

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Đăng nhập
    public User login(String account, String password) {

        // Kiểm tra admin trước — không cần DB
        if (ADMIN_ACCOUNT.equals(account) && ADMIN_PASSWORD.equals(password)) {
            return new Admin(ADMIN_ID, ADMIN_ACCOUNT, "");
        }

        // Các user bình thường → tra DB như cũ
        User user = userDAO.findByAccount(account);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    // Đăng ký user mới
    public boolean register(String account, String password) {
        if (account == null || account.isBlank()) return false;
        if (password == null || password.isBlank()) return false;

        // Không cho đăng ký trùng tên admin
        if (ADMIN_ACCOUNT.equalsIgnoreCase(account)) return false;

        User existedUser = userDAO.findByAccount(account);
        if (existedUser != null) return false;

        Bidder bidder = new Bidder(0, account, password, BigDecimal.ZERO);
        return userDAO.insert(bidder);
    }

    // Nâng cấp quyền từ BIDDER sang SELLER
    public boolean upgradeToSeller(int userId) {
        User user = userDAO.findById(userId);
        if (user == null) return false;
        if (user.getRole() == Role.SELLER) return false;
        return userDAO.upgradeToSeller(userId);
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

    // Kiểm tra role
    public boolean isRole(User user, Role role) {
        return user != null && user.getRole() == role;
    }

    public boolean isSeller(User user) {
        return user != null && user.getRole() == Role.SELLER;
    }

    public boolean isBidder(User user) {
        return user != null &&
                (user.getRole() == Role.BIDDER || user.getRole() == Role.SELLER);
    }

    public boolean canBid(User user) {
        return user != null &&
                (user.getRole() == Role.BIDDER || user.getRole() == Role.SELLER);
    }

    public boolean canSell(User user) {
        return user != null && user.getRole() == Role.SELLER;
    }
}