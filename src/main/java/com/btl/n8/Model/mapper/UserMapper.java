package com.btl.n8.Model.mapper;

import com.btl.n8.Model.entity.Admin;
import com.btl.n8.Model.entity.Bidder;
import com.btl.n8.Model.entity.Seller;
import com.btl.n8.Model.entity.User;
import com.btl.n8.Model.enums.Role;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserMapper {
    public static User map(ResultSet rs) throws SQLException {

        int id = rs.getInt("user_id");
        String account = rs.getString("account");
        String password = rs.getString("password");

        Role role = Role.valueOf(rs.getString("role"));

        switch (role) {

            case BIDDER:

                BigDecimal balance = rs.getBigDecimal("balance");

                return new Bidder(
                        id,
                        account,
                        password,
                        balance
                );

            case SELLER:

                return new Seller(
                        id,
                        account,
                        password
                );

            case ADMIN:

                return new Admin(
                        id,
                        account,
                        password
                );

            default:
                throw new IllegalArgumentException("Invalid role");
        }
    }
}
