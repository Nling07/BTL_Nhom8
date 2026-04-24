package com.btl.n8.service;

import com.btl.n8.Connection.ItemDAO;
import com.btl.n8.Model.Item;

import java.util.List;

public class ItemService {
    private final ItemDAO itemDAO;

    public ItemService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    // Thêm item mới
    public boolean addItem(Item item) {
        if (item.getName() == null || item.getName().isBlank()) {
            return false; // kiểm tra tên hợp lệ
        }
        return itemDAO.insert(item);
    }

    // Lấy item theo id
    public Item getItemById(int id) {
        return itemDAO.findById(id);
    }

    // Lấy tất cả item
    public List<Item> getAllItems() {
        return itemDAO.findAll();
    }

    // Lấy item theo seller
    public List<Item> getItemsBySeller(int sellerId) {
        return itemDAO.findBySeller(sellerId);
    }
}
