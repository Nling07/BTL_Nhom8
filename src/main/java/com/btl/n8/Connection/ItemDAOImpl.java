package com.btl.n8.Connection;

import com.btl.n8.Model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAOImpl implements ItemDAO {
    private final Connection conn;

    public ItemDAOImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public boolean insert(Item item) {
        String sql = "INSERT INTO items(name, seller_id, type, image) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getName());
            ps.setInt(2, item.getSellerId());
            ps.setString(3, item.getType().name());
            ps.setBytes(4, item.getImage()); // thêm ảnh

            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        item.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Seller không tồn tại hoặc lỗi ràng buộc");
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi insert item: " + e.getMessage());
        }

        return false;
    }

    @Override
    public Item findById(int id) {
        String sql = "SELECT * FROM items WHERE item_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapItem(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findById item: " + e.getMessage());
        }

        return null;
    }

    @Override
    public List<Item> findAll() {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM items";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapItem(rs));
            }

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findAll item: " + e.getMessage());
        }

        return list;
    }

    @Override
    public List<Item> findBySeller(int sellerId) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapItem(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findBySeller: " + e.getMessage());
        }

        return list;
    }

    private Item mapItem(ResultSet rs) throws SQLException {
        int id = rs.getInt("item_id");
        String name = rs.getString("name");
        int sellerId = rs.getInt("seller_id");
        String typeStr = rs.getString("type");
        ItemType type = ItemType.valueOf(typeStr);

        byte[] image = rs.getBytes("image"); // đọc ảnh từ DB

        switch (type) {
            case POSTER:
                return new Poster(id, name, sellerId, image);
            case FIGURE:
                return new Figure(id, name, sellerId, image);
            case CARD:
                return new Card(id, name, sellerId, image);
        }

        return null;
    }
}

