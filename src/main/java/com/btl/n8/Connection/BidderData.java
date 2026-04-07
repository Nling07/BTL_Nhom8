package com.btl.n8.Connection;
import com.btl.n8.Model.Bidder;

public class BidderData extends Data {
    public BidderData(){
        super();
    }

    // Setdata | khi người dùng mới đăng kí -> set
    public void InsertBidderData(Bidder bidder){
        try {
            String sql = "INSERT INTO bidder (account,password,balance) VALUES (?,?,?);";
            ps = conn.prepareStatement(sql);
            ps.setString(1, bidder.getAccount());
            ps.setString(2, bidder.getPassword());
            ps.setBigDecimal(3, bidder.getBalance());
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Thêm thành công!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getdata | khi người dùng cũ đăng nhập -> get
    public Bidder getBidderData(String account){
        try{
            String sql = "SELECT * FROM bidder WHERE account = ?;";
            ps = conn.prepareStatement(sql);
            ps.setString(1,account);
            rs = ps.executeQuery();

            if (rs.next()) {
                return new Bidder(rs.getInt("idbidder"), account, rs.getString("password"), rs.getBigDecimal("balance"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Update Data | khi người dùng thay đổi số dư -> update số dư tk ngay
    public void UpdateBalance(Bidder bidder){
        try{
            String sql ="UPDATE bidder SET balance = ? WHERE account = ?;";
            ps = conn.prepareStatement(sql);
            ps.setBigDecimal(1,bidder.getBalance());
            ps.setString(2, bidder.getAccount());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Update thành công!");
            }

        }catch (Exception e) {
            e.printStackTrace();
        }

    }
}
