package com.btl.n8.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientSocket {
    private static volatile ClientSocket instance;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;

    private static final String SERVER_IP   = "localhost";
    private static final int    SERVER_PORT = 9090;
    private static final Gson   gson        = new Gson();

    // CopyOnWriteArrayList — thread-safe khi iterate
    private final List<ServerResponseListener> listeners = new CopyOnWriteArrayList<>();

    private ClientSocket() {}

    public static ClientSocket getInstance() {
        if (instance == null) {
            synchronized (ClientSocket.class) {
                if (instance == null) {
                    instance = new ClientSocket();
                }
            }
        }
        return instance;
    }

    // Lazy connect — gọi khi cần
    public boolean connect() {
        if (connected) return true;
        try {
            socket = new Socket();
            // Timeout 3 giây thay vì chờ mãi
            socket.connect(new java.net.InetSocketAddress(SERVER_IP, SERVER_PORT), 3000);
            in        = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out       = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            startListening();
            System.out.println("Kết nối server thành công!");
            return true;
        } catch (IOException e) {
            System.out.println("Server chưa mở: " + e.getMessage());
            connected = false;
            return false;
        }
    }
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void addListener(ServerResponseListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(ServerResponseListener listener) {
        listeners.remove(listener);
    }

    private void startListening() {
        Thread t = new Thread(() -> {
            try {
                String json;
                while ((json = in.readLine()) != null) {
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    for (ServerResponseListener l : listeners) {
                        l.onRespone(obj);
                    }
                }
            } catch (IOException e) {
                System.out.println("Mất kết nối server!");
                connected = false;
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void sendMessage(Object request) {
        if (!isConnected()) {
            System.err.println("Chưa kết nối server!");
            return;
        }
        out.println(gson.toJson(request));
        System.out.println("Gửi: " + gson.toJson(request));
    }

    public void close() {
        try {
            connected = false;
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Lỗi đóng kết nối: " + e.getMessage());
        }
    }
}