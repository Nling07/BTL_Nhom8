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
            if (conn == null){
                conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/user", "root", "123456");
            }
            return conn;
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }
}
