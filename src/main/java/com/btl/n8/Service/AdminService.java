package com.btl.n8.Service;

import com.btl.n8.Connection.UserDAO;
import com.btl.n8.Connection.AuctionDAO;
import com.btl.n8.Connection.ItemDAO;
import com.btl.n8.Model.entity.User;
import com.btl.n8.Model.entity.Auction;
import com.btl.n8.Model.entity.Item;

import java.util.List;

public class AdminService {
    private final UserDAO userDAO;
    private final AuctionDAO auctionDAO;
    private final ItemDAO itemDAO;

    public AdminService(UserDAO userDAO, AuctionDAO auctionDAO, ItemDAO itemDAO) {
        this.userDAO = userDAO;
        this.auctionDAO = auctionDAO;
        this.itemDAO = itemDAO;
    }

    // Quản lý user
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public boolean deleteUserById(int id) {
        return userDAO.deleteById(id);
    }

    // Quản lý auction
    public List<Auction> getAllAuctions() {
        return auctionDAO.findAll();
    }

    public boolean deleteAuctionById(int id) {
        return auctionDAO.deleteById(id);
    }

    // Quản lý item
    public List<Item> getAllItems() {
        return itemDAO.findAll();
    }

    public boolean deleteItemById(int id) {
        return itemDAO.deleteById(id);
    }
}
