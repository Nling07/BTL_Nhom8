package com.btl.n8.Connection;

import com.btl.n8.Model.Entity.Bidder;
import com.btl.n8.Model.Entity.Seller;
import com.btl.n8.Model.Entity.User;
import com.btl.n8.Model.Enums.Role;
import com.btl.n8.Model.Mapper.UserMapper;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements UserDAO {

    private final Connection conn;

    // Đọc thêm frozen_balance từ bảng bidders
    private static final String BASE_SELECT = """
    SELECT u.user_id, u.account, u.password, u.role,
           b.balance,
           b.frozen_balance,
           s.seller_id
    FROM users u
    LEFT JOIN bidders b ON u.user_id = b.user_id
    LEFT JOIN sellers s ON u.user_id = s.seller_id
""";

    public UserDAOImpl(Connection conn) {
        this.conn = conn;
    }

    // ── insert ────────────────────────────────────────────────────────────────

    @Override
    public boolean insert(User user) {
        try {
            conn.setAutoCommit(false);

            if (!insertUser(user)) {
                conn.rollback();
                return false;
            }

            if (user instanceof Bidder bidder) {
                if (!insertBidder(bidder)) {
                    conn.rollback();
                    return false;
                }
            }

            if (user instanceof Seller seller) {
                if (!insertSeller(seller)) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) {
                System.out.println("Lỗi rollback: " + ex.getMessage());
            }
            System.out.println("Lỗi transaction insert: " + e.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) {
                System.out.println("Lỗi setAutoCommit: " + e.getMessage());
            }
        }
        return false;
    }

    // ── upgradeToSeller ───────────────────────────────────────────────────────

    @Override
    public boolean upgradeToSeller(int userId) {
        try {
            conn.setAutoCommit(false);

            User user = findById(userId);
            if (user == null || user.getRole() == Role.SELLER) {
                conn.rollback();
                return false;
            }

            if (!setRoleById(userId, Role.SELLER)) {
                conn.rollback();
                return false;
            }

            Seller seller = new Seller(user.getId(), user.getAccount(), user.getPassword());
            if (!insertSeller(seller)) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) {
                System.out.println("Lỗi rollback upgrade seller: " + ex.getMessage());
            }
            System.out.println("Lỗi transaction upgrade seller: " + e.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) {
                System.out.println("Lỗi setAutoCommit: " + e.getMessage());
            }
        }
        return false;
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Override
    public boolean update(User user) {
        String sql = "UPDATE users SET account = ? WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getAccount());
            ps.setInt(2, user.getId());

            if (ps.executeUpdate() > 0) {
                if (user instanceof Bidder bidder) {
                    String sql2 = """
                        INSERT INTO bidders(user_id, balance, frozen_balance) VALUES(?, ?, ?)
                        ON DUPLICATE KEY UPDATE balance = VALUES(balance), frozen_balance = VALUES(frozen_balance)
                        """;
                    try (PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                        ps2.setInt(1, user.getId());
                        ps2.setBigDecimal(2, bidder.getBalance() != null ? bidder.getBalance() : BigDecimal.ZERO);
                        ps2.setBigDecimal(3, bidder.getFrozenBalance());
                        ps2.executeUpdate();
                    }
                } else if (user.getBalance() != null) {
                    String sql2 = """
                        INSERT INTO bidders(user_id, balance, frozen_balance) VALUES(?, ?, ?)
                        ON DUPLICATE KEY UPDATE balance = VALUES(balance)
                        """;
                    try (PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                        ps2.setInt(1, user.getId());
                        ps2.setBigDecimal(2, user.getBalance());
                        ps2.setBigDecimal(3, user.getFrozenBalance());
                        ps2.executeUpdate();
                    }
                }
                return true;
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Lỗi ràng buộc khi update: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi update: " + e.getMessage());
        }
        return false;
    }

    // ── setRoleById ───────────────────────────────────────────────────────────

    @Override
    public boolean setRoleById(int userId, Role role) {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi setRoleById: " + e.getMessage());
        }
        return false;
    }

    // ── find ──────────────────────────────────────────────────────────────────

    @Override
    public User findByAccount(String account) {
        String sql = BASE_SELECT + " WHERE u.account = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UserMapper.map(rs);
            }
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findByAccount: " + e.getMessage());
        }
        return null;
    }

    @Override
    public User findById(int id) {
        String sql = BASE_SELECT + " WHERE u.user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UserMapper.map(rs);
            }
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(BASE_SELECT);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(UserMapper.map(rs));
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findAll: " + e.getMessage());
        }
        return users;
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi deleteById: " + e.getMessage());
        }
        return false;
    }

    // ── Frozen balance ────────────────────────────────────────────────────────

    /**
     * Tăng frozen_balance chỉ khi available_balance (balance - frozen_balance) >= amount.
     * Dùng conditional UPDATE để tránh race condition — atomic tại DB level.
     *
     * Trả về true nếu lock thành công, false nếu không đủ tiền.
     */
    @Override
    public boolean freezeBalance(int userId, BigDecimal amount) {
        String sql = """
            UPDATE bidders
            SET frozen_balance = frozen_balance + ?
            WHERE user_id = ?
              AND (balance - frozen_balance) >= ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, userId);
            ps.setBigDecimal(3, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi freezeBalance: " + e.getMessage());
        }
        return false;
    }

    /**
     * Giảm frozen_balance (khi bị outbid hoặc không thắng).
     * Dùng GREATEST để tránh frozen xuống âm.
     */
    @Override
    public boolean unfreezeBalance(int userId, BigDecimal amount) {
        String sql = """
            UPDATE bidders
            SET frozen_balance = GREATEST(frozen_balance - ?, 0)
            WHERE user_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi unfreezeBalance: " + e.getMessage());
        }
        return false;
    }

    /**
     * Khi thắng: trừ balance thật + giải phóng frozen cùng lúc (atomic).
     * balance    = GREATEST(balance - amount, 0)
     * frozen     = GREATEST(frozen - amount, 0)
     */
    @Override
    public boolean settleWinner(int userId, BigDecimal amount) {
        String sql = """
            UPDATE bidders
            SET balance        = GREATEST(balance - ?, 0),
                frozen_balance = GREATEST(frozen_balance - ?, 0)
            WHERE user_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi settleWinner: " + e.getMessage());
        }
        return false;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private boolean insertUser(User user) {
        String sql = "INSERT INTO users (account, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getAccount());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole().name());
            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi insertUser: " + e.getMessage());
        }
        return false;
    }

    private boolean insertBidder(Bidder bidder) {
        String sql = "INSERT INTO bidders(user_id, balance, frozen_balance) VALUES (?, ?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bidder.getId());
            ps.setBigDecimal(2, bidder.getBalance() != null ? bidder.getBalance() : BigDecimal.ZERO);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi insertBidder: " + e.getMessage());
        }
        return false;
    }

    private boolean insertSeller(Seller seller) {
        String sql = "INSERT INTO sellers(seller_id) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seller.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi insertSeller: " + e.getMessage());
        }
        return false;
    }
}