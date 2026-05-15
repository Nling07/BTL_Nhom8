package com.btl.n8.Connection;

import com.btl.n8.Model.enums.Role;
import com.btl.n8.Model.entity.User;

import java.util.List;

public interface UserDAO {
    boolean insert(User user);
    boolean upgradeToSeller(int userId);
    boolean update(User user);
    boolean setRoleById(int userId, Role role);
    User findByAccount(String account);
    User findById(int id);

    // Bổ sung cho AdminService
    List<User> findAll();          // Lấy toàn bộ user
    boolean deleteById(int id);    // Xóa user theo id
}
