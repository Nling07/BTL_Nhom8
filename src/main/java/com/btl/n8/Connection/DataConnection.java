package com.btl.n8.Connection;

import java.sql.*;

/**
 * FIX CRITICAL: connection static dùng chung giữa tất cả thread → race condition SQL.
 * Đổi sang ThreadLocal: mỗi thread có Connection riêng, an toàn hoàn toàn.
 */
public abstract class DataConnection {

    private static final String URL      = "jdbc:mysql://mainline.proxy.rlwy.net:42198/railway";
    private static final String USER     = "member2";
    private static final String PASSWORD = "123456";

    // Mỗi thread giữ Connection riêng của mình
    private static final ThreadLocal<Connection> threadConn = new ThreadLocal<>();

    private DataConnection() {}

    public static Connection getConnection() {
        try {
            Connection conn = threadConn.get();
            // isClosed() KHÔNG đủ để detect connection chết qua proxy (Railway, AWS RDS, v.v.)
            // Proxy có thể kill idle TCP connection trong vòng vài phút mà Java không biết
            // → isClosed() vẫn trả về false nhưng mọi query đều ném SQLException
            // isValid(2): gửi ping thật đến DB, timeout 2 giây — chắc chắn detect connection chết
            if (conn == null || conn.isClosed() || !conn.isValid(2)) {
                conn = DriverManager.getConnection(URL, USER, PASSWORD);
                threadConn.set(conn);
            }
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gọi khi thread kết thúc (ClientHandler.run() finally block) để giải phóng connection.
     */
    public static void closeConnection() {
        try {
            Connection conn = threadConn.get();
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ignored) {
        } finally {
            threadConn.remove();
        }
    }
}