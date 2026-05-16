package com.btl.n8.network;

import com.btl.n8.dto.*;
import com.google.gson.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import com.btl.n8.util.LocalDateTimeAdapter;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final ConcurrentHashMap<String, ClientHandler> clients;
    private final String token;

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
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}