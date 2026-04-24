package com.btl.n8.service;

import com.btl.n8.Connection.UserDAO;
import com.btl.n8.Model.Seller;
import com.btl.n8.Model.User;
import com.btl.n8.Model.Role;

public class SellerService {
    private final UserDAO userDAO;

    public SellerService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Đăng ký seller mới
    public boolean registerSeller(Seller seller) {
        User existing = userDAO.findByAccount(seller.getAccount());
        if (existing != null) {
            return false; // account đã tồn tại
        }
        return userDAO.insert(seller); // đa hình: Seller là User
    }

    // Đăng nhập
    public boolean login(String account, String password) {
        User user = userDAO.findByAccount(account);
        if (user != null && user.getRole() == Role.SELLER) {
            return user.getPassword().equals(password);
        }
        return false;
    }

    // Lấy thông tin seller theo id
    public Seller getSellerById(int id) {
        User user = userDAO.findById(id);
        if (user instanceof Seller seller && user.getRole() == Role.SELLER) {
            return seller;
        }
        return null;
    }

    // Cập nhật thông tin seller
    public boolean updateSeller(Seller seller) {
        return userDAO.update(seller); // đa hình: Seller là User
    }
}
