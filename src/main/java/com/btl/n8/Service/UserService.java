package com.btl.n8.Service;

import com.btl.n8.Connection.UserDAO;
import com.btl.n8.Model.entity.Bidder;
import com.btl.n8.Model.entity.User;
import com.btl.n8.Model.enums.Role;

import java.math.BigDecimal;

public class UserService {
    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Đăng ký user mới
    public boolean register(String account, String password) {

        // account không hợp lệ
        if (account == null || account.isBlank()) {
            return false;
        }

        // password không hợp lệ
        if (password == null || password.isBlank()) {
            return false;
        }

        // account đã tồn tại
        User existedUser =
                userDAO.findByAccount(account);

        if (existedUser != null) {
            return false;
        }

        // mặc định user mới là bidder
        Bidder bidder = new Bidder(
                0,
                account,
                password,
                BigDecimal.ZERO
        );

        return userDAO.insert(bidder);
    }

    // Nâng cấp quyền từ BIDDER sang SELLER
    public boolean upgradeToSeller(int userId) {

        User user = userDAO.findById(userId);

        if (user == null) {
            return false;
        }

        // đã là seller
        if (user.getRole() == Role.SELLER) {
            return false;
        }

        return userDAO.upgradeToSeller(userId);
    }

    // Đăng nhập
    public User login(String account, String password) {
        User user = userDAO.findByAccount(account);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    // Cập nhật thông tin user
    public boolean updateUser(User user) {
        return userDAO.update(user);
    }

    // Lấy user theo id
    public User getUserById(int id) {
        return userDAO.findById(id);
    }

    // Lấy user theo account
    public User getUserByAccount(String account) {
        return userDAO.findByAccount(account);
    }

    // Kiểm tra role của user
    public boolean isRole(User user, Role role) {
        return user != null && user.getRole() == role;
    }

    public boolean isSeller(User user) {

        return user != null &&
                user.getRole() == Role.SELLER;
    }

    public boolean isBidder(User user) {

        return user != null &&
                (user.getRole() == Role.BIDDER
                        || user.getRole() == Role.SELLER);
    }

    // Phân quyền
    public boolean canBid(User user) {

        return user != null &&
                (user.getRole() == Role.BIDDER
                        || user.getRole() == Role.SELLER);
    }

    public boolean canSell(User user) {

        return user != null &&
                user.getRole() == Role.SELLER;
    }
}
