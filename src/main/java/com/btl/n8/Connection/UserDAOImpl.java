package com.btl.n8.Connection;
import com.btl.n8.Model.*;

import java.math.BigDecimal;
import java.sql.*;

public class UserDAOImpl implements UserDAO {
    private final Connection conn;
    public UserDAOImpl(Connection conn){
        this.conn = conn;
    }

    @Override
    public boolean insert(User user) {
        String sql = "INSERT INTO users (account, password, role) VALUES (?, ?, ?)";

        // Sử dụng try-with-resources để tự động đóng ps và rs
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getAccount());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole().name());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int userId = rs.getInt(1);
                        user.setId(userId);

                        // Xử lý BIDDER
                        if (user instanceof Bidder) {
                            String sql2 = "INSERT INTO bidders(user_id, balance) VALUES (?, ?)";
                            try (PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                                ps2.setInt(1, userId);
                                ps2.setBigDecimal(2, ((Bidder) user).getBalance());
                                ps2.executeUpdate();
                            } // ps2 tự đóng ở đây
                        }

                        // Xử lý SELLER
                        if (user instanceof Seller) {
                            String sql3 = "INSERT INTO sellers(seller_id) VALUES (?)";
                            try (PreparedStatement ps3 = conn.prepareStatement(sql3)) {
                                ps3.setInt(1, userId);
                                ps3.executeUpdate();
                            } // ps3 tự đóng ở đây
                        }
                    }
                } // rs tự đóng ở đây
                return true;
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("Trùng username hoặc lỗi ràng buộc: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Lỗi SQL khi insert: " + e.getMessage());
        }

        return false;
    }

    @Override
    public boolean update(User user) {
        String sql = "UPDATE users SET account = ?, password = ? WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getAccount());
            ps.setString(2, user.getPassword());
            ps.setInt(3, user.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                // BIDDER
                if (user instanceof Bidder) {
                    String sql2 = "UPDATE bidders SET balance = ? WHERE user_id = ?";
                    try (PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                        ps2.setBigDecimal(1, ((Bidder) user).getBalance());
                        ps2.setInt(2, user.getId());
                        ps2.executeUpdate();
                    }
                }
                // SELLER không có field riêng → không cần update thêm
                return true;
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Lỗi ràng buộc");
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi update");
        }

        return false;
    }


    @Override
    public User findByAccount(String account) {
        String sql = """
        SELECT u.user_id, u.account, u.password, u.role,
               b.balance,
               s.seller_id
        FROM users u
        LEFT JOIN bidders b ON u.user_id = b.user_id
        LEFT JOIN sellers s ON u.user_id = s.seller_id
        WHERE u.account = ?
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findByAccount: " + e.getMessage());
        }

        return null;
    }

    @Override
    public User findById(int id) {
        String sql = """
        SELECT u.user_id, u.account, u.password, u.role,
               b.balance,
               s.seller_id
        FROM users u
        LEFT JOIN bidders b ON u.user_id = b.user_id
        LEFT JOIN sellers s ON u.user_id = s.seller_id
        WHERE u.user_id = ?
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findById: " + e.getMessage());
        }

        return null;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String account = rs.getString("account");
        String password = rs.getString("password");
        String role = rs.getString("role");

        if ("BIDDER".equals(role)) {
            BigDecimal balance = rs.getBigDecimal("balance");
            if (balance == null) balance = BigDecimal.ZERO;
            return new Bidder(id, account, password, balance);

        } else if ("SELLER".equals(role)) {
            return new Seller(id, account, password);

        } else {
            return new Admin(id, account, password);
        }
    }

}
