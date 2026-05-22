package com.btl.n8.Model.Mapper;

import com.btl.n8.Model.Entity.Admin;
import com.btl.n8.Model.Entity.Bidder;
import com.btl.n8.Model.Entity.Seller;
import com.btl.n8.Model.Entity.User;
import com.btl.n8.Model.Enums.Role;

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
                // Seller vẫn giữ balance từ bảng bidders (vì upgradeToSeller không xóa bidder row)
                BigDecimal sellerBalance = rs.getBigDecimal("balance");
                Seller seller = new Seller(id, account, password);
                seller.setBalance(sellerBalance);
                return seller;

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