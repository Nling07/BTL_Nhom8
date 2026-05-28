package com.btl.n8.Network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.btl.n8.Util.LocalDateTimeAdapter;
import java.time.LocalDateTime;

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
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

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

    public boolean connect() {
        if (connected) return true;
        try {
            socket = new Socket();
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

    /**
     * Reset socket về trạng thái ban đầu — giữ nguyên listeners.
     * Gọi khi server restart hoặc kết nối bị chết.
     */
    public void reset() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        socket    = null;
        in        = null;
        out       = null;
        connected = false;
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
            } finally {
                // Tự reset khi socket chết → lần login tiếp theo connect lại được
                reset();
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