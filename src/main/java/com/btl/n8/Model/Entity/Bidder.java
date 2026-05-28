package com.btl.n8.Model.Entity;

import com.btl.n8.Model.Enums.Role;
import java.math.BigDecimal;

public class Bidder extends User {

    public Bidder() {}

    public Bidder(String account, String password, BigDecimal balance) {
        super(account, password, Role.BIDDER);
        this.balance = balance;          // dùng User.balance trực tiếp
    }

    public Bidder(int id, String account, String password, BigDecimal balance) {
        // FIX: gọi constructor User có balance param → User.balance được set đúng
        // Trước đây gọi super(id, acc, pw, Role.BIDDER) — không truyền balance
        // → User.balance = null, Bidder.balance = giá trị đúng (2 field tách biệt)
        // → winner.getBalance() trả về null → NPE hoặc balance không bị trừ
        super(id, account, password, Role.BIDDER, balance);
    }

    // KHÔNG khai báo lại field balance, KHÔNG override getBalance/setBalance
    // → kế thừa hoàn toàn từ User → chỉ có 1 field balance duy nhất
}