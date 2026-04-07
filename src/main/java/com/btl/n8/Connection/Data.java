package com.btl.n8.Connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Data {
    protected Connection conn;
    protected DriverManager dr;
    protected PreparedStatement ps;
    protected ResultSet rs;
    public Data(){
        String url = "jdbc:mysql://localhost:3306/user";
        String user = "root";
        String password = "123456";

        try {
            conn = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
