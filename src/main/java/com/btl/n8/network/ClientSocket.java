package com.btl.n8.Network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientSocket {
    // danh sach các controller đang lắng nghe
    private final List<ServerResponseListener> listeners = new ArrayList<>();

    private static ClientSocket instance;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private static final String  SERVER_IP = "localhost";
    private static final int SERVER_PORT = 9090;
    private static final Gson gson = new Gson();

    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }
    private  ClientSocket()  {
        try {
            this.socket = new Socket(SERVER_IP,SERVER_PORT);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(),true);
            startListening();
        } catch (IOException e) {
            System.out.println("Server chua mở máy chủ");
        }
    }
    // Hàm để Controller đăng ký nhận tin
    public void addListener(ServerResponseListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    // Hàm để Controller hủy đăng ký khi đóng màn hình (tránh rò rỉ bộ nhớ)
    public void removeListener(ServerResponseListener listener) {
        listeners.remove(listener);
    }

    /*
    Đây là hàm tạo ra để chạy trên 1 thread khác(tránh main thread),
    hàm này sẽ liên tục lắng nghe yêu cầu từ Server
    Sau khi đọc được yêu cầu từ Server(chuỗi json) thì nó sẽ chuyển về JsonObject để thực hiện gửi JsonObject và xử lí đến controller
    nào đang cần dữ liệu (cái này nằm trong observer pattern)
     */
    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try{
                String json;
                while ((json = in.readLine()) != null){
                    JsonObject Jobject = JsonParser.parseString(json).getAsJsonObject();//parse String json thành element rồi biến nó thành json obj
                     for (ServerResponseListener listener : listeners){
                         listener.onRespone(Jobject);
                     }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        listenerThread.setDaemon(true); //khi tắt app thì hàm nghe mới tắt
        listenerThread.start();
    }

    //Hàm này để gửi đối tượng request từ controller đến client.
    public void sendMessage(Object request) {
        if (out == null) {
            System.err.println("Chưa kết nối server!");
            return;
        }
        String json = gson.toJson(request);
        out.println(json);
        System.out.println("Gửi lên server: " + json);
    }

    //Hàm đóng socket.
    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Lỗi đóng kết nối: " + e.getMessage());
        }
    }

}
