package com.btl.n8.service;

import com.btl.n8.Connection.UserDAO;
import com.btl.n8.Model.Bidder;
import com.btl.n8.Model.User;
import com.btl.n8.Model.Role;

import java.math.BigDecimal;

public class BidderService {
    private final UserDAO userDAO;

    public BidderService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Đăng ký bidder mới
    public boolean registerBidder(Bidder bidder) {
        User existing = userDAO.findByAccount(bidder.getAccount());
        if (existing != null) {
            return false; // account đã tồn tại
        }
        return userDAO.insert(bidder); // đa hình: Bidder là User
    }

    // Đăng nhập
    public boolean login(String account, String password) {
        User user = userDAO.findByAccount(account);
        if (user != null && user.getRole() == Role.BIDDER) {
            return user.getPassword().equals(password);
        }
        return false;
    }

    // Lấy thông tin bidder theo id
    public Bidder getBidderById(int id) {
        User user = userDAO.findById(id);
        if (user instanceof Bidder bidder && user.getRole() == Role.BIDDER) {
            return bidder;
        }
        return null;
    }
    public Bidder getBidderByAccount(String account) {
        User user = userDAO.findByAccount(account);
        if (user instanceof Bidder bidder && user.getRole() == Role.BIDDER) {
            return bidder;
        }
        return null;
    }

    // Cập nhật số dư
    public boolean updateBalance(int bidderId, BigDecimal newBalance) {
        User user = userDAO.findById(bidderId);
        if (user instanceof Bidder bidder && user.getRole() == Role.BIDDER) {
            bidder.setBalance(newBalance);
            return userDAO.update(bidder);
        }
        return false;
    }
}
