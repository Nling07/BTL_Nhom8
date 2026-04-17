package com.btl.n8.Connection;

import com.btl.n8.Model.User;

public interface UserDAO {
    boolean insert(User user);
    boolean update(User user);
    User findByAccount(String account);
    User findById(int id);
}
