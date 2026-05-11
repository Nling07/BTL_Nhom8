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
                String url = "jdbc:mysql://10.228.45.83/pokemon_db";
                String user = "member2";
                String password = "123456";
                Connection conn = DriverManager.getConnection(url, user, password);
            }
            return conn;
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }
}
