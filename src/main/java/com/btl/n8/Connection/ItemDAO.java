package com.btl.n8.Connection;

import com.btl.n8.Model.Item;

import java.util.List;

public interface ItemDAO {
    boolean insert(Item item);
    Item findById(int id);
    List<Item> findAll();
    List<Item> findBySeller(int sellerId);

}
