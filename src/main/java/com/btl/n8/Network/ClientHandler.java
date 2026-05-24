package com.btl.n8.Network;

import com.btl.n8.Connection.DataConnection;
import com.btl.n8.DTO.*;
import com.google.gson.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import com.btl.n8.Util.LocalDateTimeAdapter;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final ConcurrentHashMap<String, ClientHandler> clients;
    private final String token;           // UUID khi kết nối TCP — dùng làm key trong clients map

    /**
     * FIX CRITICAL: loginSessionId là sessionId được tạo khi user LOGIN thành công.
     * Đây là key trong ServerSessionManager.sessions.
     * Hai key này (token vs loginSessionId) hoàn toàn khác nhau — trước đây
     * SettlementHandler dùng token để tìm trong ServerSessionManager → luôn null
     * → isWinner không bao giờ được set → không client nào biết mình thắng.
     */
    private volatile String loginSessionId = null;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private final RequestHandler requestHandler;

    public ClientHandler(Socket socket,
                         ConcurrentHashMap<String, ClientHandler> clients,
                         String token) throws IOException {
        this.socket  = socket;
        this.clients = clients;
        this.token   = token;
        this.in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);

        this.requestHandler = new RequestHandler(this, clients);
    }

    public void send(String json) {
        out.println(json);
    }

    /** Gọi từ RequestHandler.handleLogin() sau khi tạo session thành công. */
    public void setLoginSessionId(String sessionId) {
        this.loginSessionId = sessionId;
    }

    /**
     * Trả về loginSessionId — dùng trong SettlementHandler để tra cứu
     * user trong ServerSessionManager đúng key.
     */
    public String getLoginSessionId() {
        return loginSessionId;
    }

    @Override
    public void run() {
        try {
            String json;
            while ((json = in.readLine()) != null) {
                try {
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    String action  = obj.get("action").getAsString();
                    switch (action) {
                        case "LOGIN"     -> requestHandler.handleLogin(gson.fromJson(json, LoginRequest.class));
                        case "REGISTER"  -> requestHandler.handleRegister(gson.fromJson(json, RegisterRequest.class));
                        case "BID"       -> requestHandler.handleBid(gson.fromJson(json, BidRequest.class));
                        case "ADD_ITEM"  -> requestHandler.handleAddItem(gson.fromJson(json, AddItemRequest.class));
                        case "AUTO_BID"  -> requestHandler.handleAutoBid(gson.fromJson(json, AutoBidRequest.class));
                    }
                } catch (JsonSyntaxException e) {
                    System.err.println("JSON lỗi: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Client ngắt kết nối: " + token);
        } finally {
            clients.remove(token);
            DataConnection.closeConnection();
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}