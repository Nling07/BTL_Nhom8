package com.btl.n8.Connection;

import com.btl.n8.Model.Bid;
import com.btl.n8.Model.BidStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDAOImpl implements BidDAO {
    private final Connection conn;

    public BidDAOImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public boolean insert(Bid bid) {
        String sql = """
        INSERT INTO bids(auction_id, bidder_id, amount, bid_time, status)
        VALUES (?, ?, ?, ?, ?)
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, bid.getAuctionId());
            ps.setInt(2, bid.getBidderId());
            ps.setBigDecimal(3, bid.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(bid.getBidTime()));
            ps.setString(5, bid.getStatus().name());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        bid.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Auction hoặc bidder không tồn tại");
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi insert bid: " + e.getMessage());
        }

        return false;
    }

    @Override
    public List<Bid> findByAuction(int auctionId) {
        List<Bid> list = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY amount DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBid(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findByAuction: " + e.getMessage());
        }

        return list;
    }

    @Override
    public Bid findHighestBid(int auctionId) {
        String sql = """
        SELECT * FROM bids 
        WHERE auction_id = ?
        ORDER BY amount DESC
        LIMIT 1
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBid(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findHighestBid: " + e.getMessage());
        }

        return null;
    }

    @Override
    public boolean updateStatus(int bidId, BidStatus status) {
        String sql = "UPDATE bids SET status = ? WHERE bid_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, bidId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi update status: " + e.getMessage());
        }

        return false;
    }

    @Override
    public boolean updateOutbid(int auctionId) {
        String sql = """
        UPDATE bids 
        SET status = 'OUTBID' 
        WHERE auction_id = ? AND status = 'ACTIVE'
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);

            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi update OUTBID: " + e.getMessage());
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


