package com.btl.n8.Connection;
import com.btl.n8.Model.Role;
import com.btl.n8.Model.User;

import java.sql.*;

public class UserDAOImpl implements UserDAO {
    private Connection conn = DataConnection.getConnection();
    public UserDAOImpl(){
    }

    @Override
    public boolean insert(User user){
        try {
            String sql = "INSERT INTO user (account,password,role) VALUES (?,?,?);";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user.getAccount());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole().name());
            int rows = ps.executeUpdate();

            if(rows > 0){
                ResultSet rs = ps.executeQuery();
                if (rs.next()){
                    user.setId(rs.getInt("id"));
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }



    @Override
    public boolean update(User user) {
        String sql = "UPDATE user SET account = ?, password = ?, role = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getAccount());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole().name());
            ps.setInt(4, user.getId());

            int rows = ps.executeUpdate();

            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public User findByAccount(String account){
        try{
            String sql = "SELECT * FROM user WHERE account = ?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1,account);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("account"), rs.getString("password"), Role.valueOf(rs.getString("role")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public User findById(int id){
        try{
            String sql = "SELECT * FROM user WHERE id = ?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1,id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("account"), rs.getString("password"), Role.valueOf(rs.getString("role")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Update Data | khi người dùng thay đổi số dư -> update số dư tk ngay
//    public void UpdateBalance(Bidder bidder){
//        try{
//            String sql ="UPDATE user SET balance = ? WHERE id = ?;";
//            ps = conn.prepareStatement(sql);
//            ps.setBigDecimal(1,bidder.getBalance());
//            ps.setInt(2, bidder.getId());
//            int rows = ps.executeUpdate();
//            if (rows > 0) {
//                System.out.println("Update thành công!");
//            }
//
//        }catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
}
