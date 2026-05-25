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
    private static final String ADMIN_ACCOUNT  = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final int    ADMIN_ID       = 0;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // ── Đăng nhập ─────────────────────────────────────────────────────────────

    public User login(String account, String password) {
        if (ADMIN_ACCOUNT.equals(account) && ADMIN_PASSWORD.equals(password)) {
            return new Admin(ADMIN_ID, ADMIN_ACCOUNT, "");
        }
        User user = userDAO.findByAccount(account);
        if (user != null && user.getPassword().equals(password)) return user;
        return null;
    }

    // ── Đăng ký ───────────────────────────────────────────────────────────────

    public boolean register(String account, String password) {
        if (account == null || account.isBlank()) return false;
        if (password == null || password.isBlank()) return false;
        if (ADMIN_ACCOUNT.equalsIgnoreCase(account)) return false;
        if (userDAO.findByAccount(account) != null) return false;
        return userDAO.insert(new Bidder(0, account, password, BigDecimal.ZERO));
    }

    // ── Nâng cấp lên Seller ───────────────────────────────────────────────────

    public boolean upgradeToSeller(int userId) {
        User user = userDAO.findById(userId);
        if (user == null || user.getRole() == Role.SELLER) return false;
        return userDAO.upgradeToSeller(userId);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public boolean updateUser(User user) { return userDAO.update(user); }
    public User getUserById(int id)      { return userDAO.findById(id); }
    public User getUserByAccount(String account) { return userDAO.findByAccount(account); }

    // ── Role check ────────────────────────────────────────────────────────────

    public boolean isRole(User user, Role role) { return user != null && user.getRole() == role; }
    public boolean isSeller(User user)          { return user != null && user.getRole() == Role.SELLER; }
    public boolean isBidder(User user)          { return user != null && (user.getRole() == Role.BIDDER || user.getRole() == Role.SELLER); }
    public boolean canBid(User user)            { return isBidder(user); }
    public boolean canSell(User user)           { return user != null && user.getRole() == Role.SELLER; }

    // ── Frozen balance ────────────────────────────────────────────────────────

    /**
     * Khóa tiền khi người dùng đặt giá mới (trước khi ghi bid vào DB).
     * Trả về false nếu available_balance không đủ → từ chối bid.
     */
    public boolean freezeBalance(int userId, BigDecimal amount) {
        return userDAO.freezeBalance(userId, amount);
    }

    /**
     * Giải phóng tiền đã khóa khi bid cũ bị outbid.
     * oldAmount = giá bid cũ của user ở auction này (lấy từ bid record).
     */
    public boolean unfreezeBalance(int userId, BigDecimal amount) {
        return userDAO.unfreezeBalance(userId, amount);
    }

    /**
     * Khi thắng đấu giá: trừ balance thật và giải phóng frozen cùng lúc.
     */
    public boolean settleWinner(int userId, BigDecimal amount) {
        return userDAO.settleWinner(userId, amount);
    }
}