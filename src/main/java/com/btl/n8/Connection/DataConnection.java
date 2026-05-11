package com.btl.n8.Connection;

import java.sql.*;

public abstract class DataConnection {
    protected static Connection conn;
    protected DriverManager dr;
    protected PreparedStatement ps;
    protected ResultSet rs;
    private DataConnection(){
    }

    public static Connection getConnection(){
        try{
            if (conn == null || conn.isClosed()){
                String url = "jdbc:mysql://mainline.proxy.rlwy.net:42198/railway";
                String user = "member2"; // hoặc root nếu bạn dùng root
                String password = "123456"; // mật khẩu bạn đã đặt
                conn = DriverManager.getConnection(url, user, password);
            }
            return conn;
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }
}
