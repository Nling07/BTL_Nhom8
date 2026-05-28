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
import java.util.concurrent.atomic.AtomicBoolean;

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

    // Guard chống multiple listening threads khi connect() bị gọi nhiều lần
    private final AtomicBoolean listening = new AtomicBoolean(false);

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
        // Nếu đã có 1 thread đang lắng nghe thì không tạo thêm —
        // tránh tình trạng connect() bị gọi nhiều lần sinh ra nhiều thread
        // cùng đọc 1 stream, khiến message bị "nuốt" bởi thread sai.
        if (!listening.compareAndSet(false, true)) {
            System.out.println("[ClientSocket] Listening thread đã chạy, bỏ qua.");
            return;
        }

        Thread t = new Thread(() -> {
            try {
                String json;
                while ((json = in.readLine()) != null) {
                    final String line = json;
                    try {
                        // Parse 1 lần để validate JSON hợp lệ
                        JsonParser.parseString(line).getAsJsonObject();

                        // Mỗi listener nhận 1 bản parse riêng — tránh 1 listener
                        // modify object ảnh hưởng đến listener tiếp theo.
                        for (ServerResponseListener l : listeners) {
                            try {
                                JsonObject copy = JsonParser.parseString(line).getAsJsonObject();
                                l.onRespone(copy);
                            } catch (Exception listenerEx) {
                                // 1 listener crash KHÔNG làm chết listening thread
                                System.err.println("[ClientSocket] Listener error: " + listenerEx.getMessage());
                            }
                        }
                    } catch (com.google.gson.JsonSyntaxException jsonEx) {
                        // JSON lỗi → bỏ qua message này, KHÔNG dừng thread
                        System.err.println("[ClientSocket] JSON parse error (skipped): " + jsonEx.getMessage());
                    }
                }
            } catch (IOException e) {
                System.out.println("Mất kết nối server!");
            } finally {
                listening.set(false); // cho phép tạo lại thread khi connect() lại
                reset();
            }
        });
        t.setDaemon(true);
        t.setName("ClientSocket-listener");
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