package com.btl.n8.Connection;

import com.btl.n8.Controller.BidController.ItemAuctionRow;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Enums.AuctionStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAOImpl implements AuctionDAO {
    private final Connection conn;

    public AuctionDAOImpl(Connection conn) {
        this.conn = conn;
    }

    // ── findAllWithItems ──────────────────────────────────────────────────────

    public List<ItemAuctionRow> findAllWithItems() {
        List<ItemAuctionRow> result = new ArrayList<>();
        String sql = """
            SELECT
                i.item_id,
                i.name       AS item_name,
                i.type       AS item_type,
                a.auction_id,
                a.current_price,
                a.status
            FROM items i
            LEFT JOIN auctions a ON a.item_id = i.item_id
            ORDER BY i.item_id
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int itemId         = rs.getInt("item_id");
                String itemName    = rs.getString("item_name");
                String itemType    = rs.getString("item_type");
                int auctionId      = rs.getInt("auction_id");
                boolean hasAuction = !rs.wasNull();
                BigDecimal price   = rs.getBigDecimal("current_price");
                String status      = rs.getString("status");
                result.add(new ItemAuctionRow(
                        itemId, itemName, itemType,
                        hasAuction ? price  : null,
                        hasAuction ? status : null,
                        hasAuction ? auctionId : -1
                ));
            }
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findAllWithItems: " + e.getMessage());
        }
        return result;
    }

    // ── closeExpiredAuctions ──────────────────────────────────────────────────

    public int closeExpiredAuctions() {
        String sql = """
            UPDATE auctions
            SET status = 'CLOSED'
            WHERE status = 'OPEN'
              AND end_time < NOW()
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi closeExpiredAuctions: " + e.getMessage());
        }
        return 0;
    }

    // ── insert ────────────────────────────────────────────────────────────────

    @Override
    public boolean insert(Auction auction) {
        String sql = """
        INSERT INTO auctions(item_id, starting_price, current_price, start_time, end_time, status)
        VALUES (?, ?, ?, ?, ?, ?)
    """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, auction.getItemId());
            ps.setBigDecimal(2, auction.getStartingPrice());
            ps.setBigDecimal(3, auction.getCurrentPrice());
            ps.setTimestamp(4, Timestamp.valueOf(auction.getStartTime()));
            ps.setTimestamp(5, Timestamp.valueOf(auction.getEndTime()));
            ps.setString(6, auction.getStatus().name());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) auction.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Item không tồn tại hoặc đã có auction");
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi insert auction: " + e.getMessage());
        }
        return false;
    }

    @Override
    public Auction findById(int id) {
        String sql = "SELECT * FROM auctions WHERE auction_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAuction(rs);
            }
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findById auction: " + e.getMessage());
        }
        return null;
    }

    @Override
    public Auction findByItemId(int itemId) {
        String sql = "SELECT * FROM auctions WHERE item_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAuction(rs);
            }
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findByItemId: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean update(Auction auction) {
        String sql = """
        UPDATE auctions
        SET starting_price = ?, current_price = ?, start_time = ?, end_time = ?, status = ?
        WHERE auction_id = ?
    """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, auction.getStartingPrice());
            ps.setBigDecimal(2, auction.getCurrentPrice());
            ps.setTimestamp(3, Timestamp.valueOf(auction.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(auction.getEndTime()));
            ps.setString(5, auction.getStatus().name());
            ps.setInt(6, auction.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi update auction: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean updateCurrentPrice(int auctionId, BigDecimal price) {
        String sql = "UPDATE auctions SET current_price = ? WHERE auction_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, price);
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi updateCurrentPrice: " + e.getMessage());
        }
        return false;
    }

    /**
     * Anti-sniping: gia hạn end_time.
     * Chỉ update nếu auction vẫn đang OPEN để tránh gia hạn nhầm auction đã đóng.
     */
    @Override
    public boolean extendEndTime(int auctionId, LocalDateTime newEndTime) {
        String sql = """
            UPDATE auctions
            SET end_time = ?
            WHERE auction_id = ?
              AND status = 'OPEN'
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(newEndTime));
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi extendEndTime: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM auctions WHERE auction_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi delete auction: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<Auction> findAll() {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions ORDER BY auction_id";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) auctions.add(mapAuction(rs));
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findAll auctions: " + e.getMessage());
        }
        return auctions;
    }

    private Auction mapAuction(ResultSet rs) throws SQLException {
        int id               = rs.getInt("auction_id");
        int itemId           = rs.getInt("item_id");
        BigDecimal starting  = rs.getBigDecimal("starting_price");
        BigDecimal current   = rs.getBigDecimal("current_price");
        LocalDateTime start  = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime end    = rs.getTimestamp("end_time").toLocalDateTime();
        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
        return new Auction(id, itemId, starting, current, start, end, status);
    }
}