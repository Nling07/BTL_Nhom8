package com.btl.n8.Connection;

import com.btl.n8.Model.Auction;
import com.btl.n8.Model.AuctionStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

public class AuctionDAOImpl implements AuctionDAO{
    private Connection conn;

    public AuctionDAOImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public boolean insert(Auction auction) {
        try {
            String sql = """
                INSERT INTO auctions(item_id, starting_price, current_price, start_time, end_time, status)
                VALUES (?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, auction.getItemId());
            ps.setBigDecimal(2, auction.getStartingPrice());
            ps.setBigDecimal(3, auction.getCurrentPrice());
            ps.setTimestamp(4, Timestamp.valueOf(auction.getStartTime()));
            ps.setTimestamp(5, Timestamp.valueOf(auction.getEndTime()));
            ps.setString(6, auction.getStatus().name());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    auction.setId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Item không tồn tại hoặc đã có auction");
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi insert auction");
        }

        return false;
    }

    @Override
    public Auction findById(int id) {
        try {
            String sql = "SELECT * FROM auctions WHERE auction_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapAuction(rs);
            }

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findById auction");
        }

        return null;
    }

    @Override
    public Auction findByItemId(int itemId) {
        try {
            String sql = "SELECT * FROM auctions WHERE item_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapAuction(rs);
            }

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findByItemId");
        }

        return null;
    }

    @Override
    public boolean update(Auction auction) {
        try {
            String sql = """
                UPDATE auctions 
                SET starting_price = ?, current_price = ?, start_time = ?, end_time = ?, status = ?
                WHERE auction_id = ?
            """;

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setBigDecimal(1, auction.getStartingPrice());
            ps.setBigDecimal(2, auction.getCurrentPrice());
            ps.setTimestamp(3, Timestamp.valueOf(auction.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(auction.getEndTime()));
            ps.setString(5, auction.getStatus().name());
            ps.setInt(6, auction.getId());

            int rows = ps.executeUpdate();

            return rows > 0;

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi update auction");
        }

        return false;
    }

    @Override
    public boolean updateCurrentPrice(int auctionId, BigDecimal price) {
        try {
            String sql = "UPDATE auctions SET current_price = ? WHERE auction_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setBigDecimal(1, price);
            ps.setInt(2, auctionId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi update current price");
        }

        return false;
    }

    private Auction mapAuction(ResultSet rs) throws SQLException {
        int id = rs.getInt("auction_id");
        int itemId = rs.getInt("item_id");

        BigDecimal startingPrice = rs.getBigDecimal("starting_price");
        BigDecimal currentPrice = rs.getBigDecimal("current_price");

        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();

        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));

        return new Auction(id, itemId, startingPrice, currentPrice, startTime, endTime, status);
    }
}

