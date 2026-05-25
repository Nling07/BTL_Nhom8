package com.btl.n8.Connection;

import com.btl.n8.Model.Enums.Role;
import com.btl.n8.Model.Entity.User;

import java.math.BigDecimal;
import java.util.List;

public interface UserDAO {
    boolean insert(User user);
    boolean upgradeToSeller(int userId);
    boolean update(User user);
    boolean setRoleById(int userId, Role role);
    User findByAccount(String account);
    User findById(int id);

    // Bổ sung cho AdminService
    List<User> findAll();
    boolean deleteById(int id);

    // ── Frozen balance (khóa tiền khi đang đấu giá) ──────────────────────────

    /**
     * Tăng frozen_balance lên amount (khi người dùng đặt giá mới).
     * Trả về false nếu available_balance không đủ.
     */
    boolean freezeBalance(int userId, BigDecimal amount);

    /**
     * Giảm frozen_balance đi amount (khi bị outbid hoặc auction kết thúc không thắng).
     */
    boolean unfreezeBalance(int userId, BigDecimal amount);

    /**
     * Khi thắng đấu giá: trừ balance thật + giải phóng frozen cùng lúc (atomic).
     * Dùng GREATEST để tránh âm.
     */
    boolean settleWinner(int userId, BigDecimal amount);
}