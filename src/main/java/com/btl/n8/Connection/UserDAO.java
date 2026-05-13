package com.btl.n8.Connection;

import com.btl.n8.Model.Bidder;
import com.btl.n8.Model.Role;
import com.btl.n8.Model.Seller;
import com.btl.n8.Model.User;

public interface UserDAO {
    boolean insert(User user);
    boolean upgradeToSeller(int userId);
    boolean update(User user);
    boolean setRoleById(int userId, Role role);
    User findByAccount(String account);
    User findById(int id);
}
