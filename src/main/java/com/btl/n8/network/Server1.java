package com.btl.n8.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server1 {
    private static final int PORT = 9090;
    public static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private ExecutorService pool = Executors.newFixedThreadPool(10);

    public Server1() throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("Server đang chạy trên port " + PORT);

        while (true) {
            Socket clientSocket = server.accept();
            System.out.println("Client mới: " + clientSocket.getInetAddress());
            try {
                String token = UUID.randomUUID().toString(); // Fix token null
                ClientHandler handler = new ClientHandler(clientSocket, clients, token);
                clients.put(token, handler);
                pool.execute(handler);
            } catch (IOException e) {
                System.err.println("Lỗi tạo ClientHandler: " + e.getMessage());
                clientSocket.close();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Server1();
    }
}