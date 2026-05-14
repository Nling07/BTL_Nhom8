package com.btl.n8.Connection;
import com.btl.n8.Model.entity.Admin;
import com.btl.n8.Model.entity.Bidder;
import com.btl.n8.Model.entity.Seller;
import com.btl.n8.Model.entity.User;
import com.btl.n8.Model.enums.Role;
import com.btl.n8.Model.mapper.UserMapper;

import java.math.BigDecimal;
import java.sql.*;

public class UserDAOImpl implements UserDAO {
    private final Connection conn;
    private static final String BASE_SELECT = """
    SELECT u.user_id, u.account, u.password, u.role,
           b.balance,
           s.seller_id
    FROM users u
    LEFT JOIN bidders b ON u.user_id = b.user_id
    LEFT JOIN sellers s ON u.user_id = s.seller_id
""";

    public UserDAOImpl(Connection conn){
        this.conn = conn;
    }

    @Override
    public boolean insert(User user) {

        try {

            conn.setAutoCommit(false);

            boolean insertedUser = insertUser(user);

            if (!insertedUser) {
                conn.rollback();
                return false;
            }

            if (user instanceof Bidder bidder) {

                boolean insertedBidder =
                        insertBidder(bidder);

                if (!insertedBidder) {
                    conn.rollback();
                    return false;
                }
            }

            if (user instanceof Seller seller) {

                boolean insertedSeller =
                        insertSeller(seller);

                if (!insertedSeller) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {

            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            System.out.println("Lỗi transaction: " + e.getMessage());
        }
        finally {

            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public boolean upgradeToSeller(int userId) {

        try {

            conn.setAutoCommit(false);

            User user = findById(userId);

            if (user == null) {
                conn.rollback();
                return false;
            }

            // đã là seller
            if (user.getRole() == Role.SELLER) {
                conn.rollback();
                return false;
            }

            // update role
            boolean updatedRole =
                    setRoleById(userId, Role.SELLER);

            if (!updatedRole) {
                conn.rollback();
                return false;
            }

            // insert seller table
            Seller seller = new Seller(
                    user.getId(),
                    user.getAccount(),
                    user.getPassword()
            );

            boolean insertedSeller =
                    insertSeller(seller);

            if (!insertedSeller) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;

        } catch (SQLException e) {

            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            System.out.println("Lỗi transaction upgrade seller: "
                    + e.getMessage());

        } finally {

            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
    public boolean setRoleById(int userId, Role role) {

        String sql = "UPDATE users SET role = ? WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role.name());
            ps.setInt(2, userId);

            int rows = ps.executeUpdate();

            return rows > 0;

        } catch (SQLException e) {

            System.out.println("Lỗi SQL khi update role: " + e.getMessage());
        }

        return false;
    }

    @Override
    public User findByAccount(String account) {
        String sql = BASE_SELECT + " WHERE u.account = ? ";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UserMapper.map(rs);
                }
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
                if (rs.next()) {
                    return UserMapper.map(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findById: " + e.getMessage());
        }

        return null;
    }


    private boolean insertUser(User user) {

        String sql = "INSERT INTO users (account, password, role) VALUES (?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getAccount());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole().name());

            int rows = ps.executeUpdate();

            if (rows > 0) {

                try (ResultSet rs = ps.getGeneratedKeys()) {

                    if (rs.next()) {

                        int userId = rs.getInt(1);

                        // gán id lại cho object
                        user.setId(userId);

                        return true;
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi insert user: " + e.getMessage());
        }

        return false;
    }

    private boolean insertBidder(Bidder bidder) {

        String sql = "INSERT INTO bidders(user_id, balance) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bidder.getId());
            ps.setBigDecimal(2, bidder.getBalance());

            int rows = ps.executeUpdate();

            return rows > 0;

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi insert bidder: " + e.getMessage());
        }

        return false;
    }

    private boolean insertSeller(Seller seller) {

        String sql = "INSERT INTO sellers(seller_id) VALUES (?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, seller.getId());

            int rows = ps.executeUpdate();

            return rows > 0;

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi insert seller: " + e.getMessage());
        }

        return false;
    }

}
