package com.btl.n8.Connection;

import java.sql.*;

public abstract class DataConnection {
    protected static Connection conn;
    private DataConnection(){
    }

    public static Connection getConnection(){
        try{
            if (conn == null){
                conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/pokemon_db", "root", "123456");
            }
            return conn;
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }
}
