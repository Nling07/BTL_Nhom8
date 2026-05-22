package com.btl.n8.Network;

import com.btl.n8.Connection.AuctionDAOImpl;
import com.btl.n8.Connection.DataConnection;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Enums.AuctionStatus;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server1 {
    private static final int PORT = 9090;
    public static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private ExecutorService pool = Executors.newFixedThreadPool(10);

    public Server1() throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("Server đang chạy trên port " + PORT);

        // ── Scheduler: cứ mỗi 10 giây kiểm tra auction hết giờ và settle ──────
        startAuctionSettlementScheduler();

        while (true) {
            Socket clientSocket = server.accept();
            System.out.println("Client mới: " + clientSocket.getInetAddress());
            try {
                String token = UUID.randomUUID().toString();
                ClientHandler handler = new ClientHandler(clientSocket, clients, token);
                clients.put(token, handler);
                pool.execute(handler);
            } catch (IOException e) {
                System.err.println("Lỗi tạo ClientHandler: " + e.getMessage());
                clientSocket.close();
            }
        }
    }

    /**
     * Mỗi 10 giây quét các auction OPEN đã quá end_time → settle.
     * Đảm bảo winner luôn được xác định và balance bị trừ đúng giờ,
     * dù không có bid nào đến sau khi auction hết giờ.
     */
    private void startAuctionSettlementScheduler() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auction-settler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                Connection conn = DataConnection.getConnection();
                if (conn == null) return;

                AuctionDAOImpl auctionDAO = new AuctionDAOImpl(conn);
                List<Auction> allAuctions = auctionDAO.findAll();
                LocalDateTime now = LocalDateTime.now();

                for (Auction auction : allAuctions) {
                    if (auction.getStatus() == AuctionStatus.OPEN
                            && now.isAfter(auction.getEndTime())) {

                        System.out.println("[Scheduler] Settling expired auction id="
                                + auction.getId());

                        // Dùng một RequestHandler tạm (không gắn với client cụ thể)
                        // để tái dùng logic settleAuction
                        SettlementHandler settler = new SettlementHandler(clients);
                        settler.settleAuction(auction.getId());
                    }
                }
            } catch (Exception e) {
                System.err.println("[Scheduler] Lỗi khi settle: " + e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws IOException {
        new Server1();
    }
}