package com.btl.n8.Service;

import com.btl.n8.Connection.ItemDAO;
import com.btl.n8.Model.*;

import java.util.List;
import java.util.stream.Collectors;

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

    // Lấy tất cả item theo loại
    public List<Item> getItemsByType(ItemType type) {
        return itemDAO.findAll().stream()
                .filter(i -> i.getType() == type)
                .collect(Collectors.toList());
    }

    // Lấy item theo seller và loại
    public List<Item> getItemsBySellerAndType(int sellerId, ItemType type) {
        return itemDAO.findBySeller(sellerId).stream()
                .filter(i -> i.getType() == type)
                .collect(Collectors.toList());
    }
    // Tạo item đúng loại — tách logic khỏi controller
    public Item createItem(String name, String type, int sellerId, byte[] imageBytes) {
        ItemType itemType = ItemType.valueOf(type);
        return switch (itemType) {
            case POSTER -> new Poster(name, sellerId, imageBytes);
            case FIGURE -> new Figure(name, sellerId, imageBytes);
            case CARD -> new Card(name, sellerId, imageBytes);
        };
    }
}
