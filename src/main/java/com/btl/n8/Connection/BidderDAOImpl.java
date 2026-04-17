package com.btl.n8.Connection;

import com.btl.n8.Model.Bidder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BidderDAOImpl implements BidderDAO{
    private Connection conn = DataConnection.getConnection();
    public BidderDAOImpl(){}

    @Override
    public boolean insert(Bidder bidder){
        String sql = "INSERT INTO bidder (user_id, balance) VALUE (?,?);";
        try (PreparedStatement ps = conn.prepareStatement(sql);) {

            ps.setInt(1, bidder.getId());
            ps.setBigDecimal(2, bidder.getBalance());
            int rows = ps.executeUpdate();

            return rows > 0;
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean updateBalance(Bidder bidder){
        String sql = "UPDATE bidder SET balance = ? WHERE id_user = ?;";
        try(PreparedStatement ps = conn.prepareStatement(sql);){

            ps.setBigDecimal(1,bidder.getBalance());
            ps.setInt(2,bidder.getId());
            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Bidder findById(int id){
        String sql = "SELECT * FROM bidder WHERE user_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return new Bidder()
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
