package com.btl.n8.Model.mapper;

import com.btl.n8.Model.entity.Item;
import com.btl.n8.Model.enums.ItemType;
import com.btl.n8.Model.factory.ItemFactory;

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

        return ItemFactory.createItem(
                type,
                id,
                name,
                sellerId,
                image
        );
    }
}
