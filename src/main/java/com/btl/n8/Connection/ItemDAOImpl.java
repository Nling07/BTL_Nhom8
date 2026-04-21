package com.btl.n8.Connection;

import com.btl.n8.Model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAOImpl implements ItemDAO {
    private Connection conn;

    public ItemDAOImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public boolean insert(Item item) {
        try {
            String sql = "INSERT INTO items(name, seller_id, type) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, item.getName());
            ps.setInt(2, item.getSellerId());
            ps.setString(3, item.getType().name());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    item.setId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Seller không tồn tại hoặc lỗi ràng buộc");
        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi insert item");
        }

        return false;
    }

    @Override
    public Item findById(int id) {
        try {
            String sql = "SELECT * FROM items WHERE item_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapItem(rs);
            }

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findById item");
        }

        return null;
    }

    @Override
    public List<Item> findAll() {
        List<Item> list = new ArrayList<>();

        try {
            String sql = "SELECT * FROM items";
            PreparedStatement ps = conn.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapItem(rs));
            }

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findAll item");
        }

        return list;
    }

    @Override
    public List<Item> findBySeller(int sellerId) {
        List<Item> list = new ArrayList<>();

        try {
            String sql = "SELECT * FROM items WHERE seller_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, sellerId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapItem(rs));
            }

        } catch (SQLException e) {
            System.out.println("Lỗi SQL khi findBySeller");
        }

        return list;
    }

    private Item mapItem(ResultSet rs) throws SQLException {
        int id = rs.getInt("item_id");
        String name = rs.getString("name");
        int sellerId = rs.getInt("seller_id");

        String typeStr = rs.getString("type");
        ItemType type = ItemType.valueOf(typeStr);

        switch (type) {
            case POSTER:
                return new Poster(id, name, sellerId);
            case FIGURE:
                return new Figure(id, name, sellerId);
            case CARD:
                return new Card(id, name, sellerId);
        }

        return null;
    }
}

