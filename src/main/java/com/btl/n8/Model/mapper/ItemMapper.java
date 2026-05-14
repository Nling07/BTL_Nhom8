package com.btl.n8.Model.mapper;

import com.btl.n8.Model.entity.Card;
import com.btl.n8.Model.entity.Figure;
import com.btl.n8.Model.entity.Item;
import com.btl.n8.Model.entity.Poster;
import com.btl.n8.Model.enums.ItemType;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ItemMapper {
    public static Item map(ResultSet rs) throws SQLException {
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
            default:
                throw new IllegalArgumentException(
                        "Invalid item type: " + type
                );
        }
    }
}
