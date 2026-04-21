package com.btl.n8.Connection;
import com.btl.n8.Model.*;

import java.math.BigDecimal;
import java.sql.*;

public class UserDAOImpl implements UserDAO {
    private Connection conn;
    public UserDAOImpl(Connection conn){
        this.conn = conn;
    }

    @Override
    public boolean insert(User user) {
        try {
            String sql = "INSERT INTO users (account, password, role) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, user.getAccount());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole().name());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();

                if (rs.next()) {
                    int userId = rs.getInt(1);
                    user.setId(userId);

                    // BIDDER
                    if (user instanceof Bidder) {
                        String sql2 = "INSERT INTO bidders(user_id, balance) VALUES (?, ?)";
                        PreparedStatement ps2 = conn.prepareStatement(sql2);

                        ps2.setInt(1, userId);
                        ps2.setBigDecimal(2, ((Bidder) user).getBalance());

                        ps2.executeUpdate();
                    }

                    // SELLER
                    if (user instanceof Seller) {
                        String sql3 = "INSERT INTO sellers(user_id) VALUES (?)";
                        PreparedStatement ps3 = conn.prepareStatement(sql3);

                        ps3.setInt(1, userId);
                        ps3.executeUpdate();
                    }
                }

                return true;
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Trùng username hoặc lỗi ràng buộc");
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi insert");
        }

        return false;
    }

    @Override
    public boolean update(User user) {
        try {
            String sql = "UPDATE users SET account = ?, password = ? WHERE user_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, user.getAccount());
            ps.setString(2, user.getPassword());
            ps.setInt(3, user.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {

                // BIDDER
                if (user instanceof Bidder) {
                    String sql2 = "UPDATE bidders SET balance = ? WHERE user_id = ?";
                    PreparedStatement ps2 = conn.prepareStatement(sql2);

                    ps2.setBigDecimal(1, ((Bidder) user).getBalance());
                    ps2.setInt(2, user.getId());

                    ps2.executeUpdate();
                }

                // SELLER không có field riêng → không cần update

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
        try {
            String sql = """
            SELECT u.user_id, u.account, u.password, u.role,
                   b.balance,
                   s.user_id AS s.seller_id
            FROM users u
            LEFT JOIN bidders b ON u.user_id = b.user_id
            LEFT JOIN sellers s ON u.user_id = s.user_id
            WHERE u.account = ?
        """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, account);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findByAccount");
        }

        return null;
    }

    @Override
    public User findById(int id) {
        try {
            String sql = """
            SELECT u.user_id, u.account, u.password, u.role,
                   b.balance,
                   s.user_id AS s.seller_id
            FROM users u
            LEFT JOIN bidders b ON u.user_id = b.user_id
            LEFT JOIN sellers s ON u.user_id = s.user_id
            WHERE u.user_id = ?
        """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findById");
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
