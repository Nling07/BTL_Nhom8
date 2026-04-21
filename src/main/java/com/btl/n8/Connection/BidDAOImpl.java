package com.btl.n8.Connection;

import com.btl.n8.Model.Bid;
import com.btl.n8.Model.BidStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDAOImpl implements BidDAO {
    private Connection conn;

    public BidDAOImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public boolean insert(Bid bid) {
        try {
            String sql = """
                INSERT INTO bids(auction_id, bidder_id, amount, bid_time, status)
                VALUES (?, ?, ?, ?, ?)
            """;

            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, bid.getAuctionId());
            ps.setInt(2, bid.getBidderId());
            ps.setBigDecimal(3, bid.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(bid.getBidTime()));
            ps.setString(5, bid.getStatus().name());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    bid.setId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Auction hoặc bidder không tồn tại");
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi insert bid");
        }

        return false;
    }

    @Override
    public List<Bid> findByAuction(int auctionId) {
        List<Bid> list = new ArrayList<>();

        try {
            String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY amount DESC";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, auctionId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapBid(rs));
            }

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findByAuction");
        }

        return list;
    }

    @Override
    public Bid findHighestBid(int auctionId) {
        try {
            String sql = """
                SELECT * FROM bids 
                WHERE auction_id = ?
                ORDER BY amount DESC
                LIMIT 1
            """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, auctionId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapBid(rs);
            }

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findHighestBid");
        }

        return null;
    }

    @Override
    public boolean updateStatus(int bidId, BidStatus status) {
        try {
            String sql = "UPDATE bids SET status = ? WHERE bid_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, status.name());
            ps.setInt(2, bidId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi update status");
        }

        return false;
    }

    @Override
    public boolean updateOutbid(int auctionId) {
        try {
            String sql = """
                UPDATE bids 
                SET status = 'OUTBID' 
                WHERE auction_id = ? AND status = 'ACTIVE'
            """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, auctionId);

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi update OUTBID");
        }

        return false;
    }

    private Bid mapBid(ResultSet rs) throws SQLException {
        int id = rs.getInt("bid_id");
        int auctionId = rs.getInt("auction_id");
        int bidderId = rs.getInt("bidder_id");

        BigDecimal amount = rs.getBigDecimal("amount");
        LocalDateTime bidTime = rs.getTimestamp("bid_time").toLocalDateTime();

        BidStatus status = BidStatus.valueOf(rs.getString("status"));

        return new Bid(id, auctionId, bidderId, amount, bidTime, status);
    }
}


