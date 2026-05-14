package com.btl.n8.network;

import com.btl.n8.Connection.*;
import com.btl.n8.dto.*;
import com.btl.n8.Model.*;
import com.btl.n8.Service.*;
import com.google.gson.*;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import com.btl.n8.util.LocalDateTimeAdapter;
import java.time.LocalDateTime;
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final ConcurrentHashMap<String, ClientHandler> clients;
    private final String token;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private AuctionService auctionService;
    private UserService userService;
    private BidService bidService;
    private ItemService itemService;

    public ClientHandler(
            Socket socket,
            ConcurrentHashMap<String, ClientHandler> clients,
            String token
    ) throws IOException {
        this.socket  = socket;
        this.clients = clients;
        this.token   = token;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);

        try {Connection conn = DataConnection.getConnection();
            userService = new UserService(new UserDAOImpl(conn));
            bidService = new BidService(new BidDAOImpl(conn));
            itemService =new ItemService(new ItemDAOImpl(conn));

            auctionService = new AuctionService(new AuctionDAOImpl(conn));

        } catch (Exception e) {
            System.err.println("Lỗi khởi tạo Service: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            String json;
            while ((json = in.readLine()) != null) {
                try {
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    String action = obj.get("action").getAsString();
                    switch (action) {
                        case "LOGIN" -> handleLogin(gson.fromJson(json, LoginRequest.class));
                        case "REGISTER" -> handleRegister(gson.fromJson(json, RegisterRequest.class));
                        case "BID" -> handleBid(gson.fromJson(json, BidRequest.class));
                        case "ADD_ITEM" -> handleAddItem(gson.fromJson(json, AddItemRequest.class));
                    }

                } catch (JsonSyntaxException e) {
                    System.err.println("JSON lỗi: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("Client ngắt kết nối: " + token);
        } finally {
            clients.remove(token);
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void handleLogin(LoginRequest req) {
        User user = userService.login(req.getUsername(), req.getPassword());
        if (user == null) {
            send(new LoginResponse("Sai tài khoản hoặc mật khẩu", null, false, -1, null, null, null));
            return;
        }

        String sessionId = ServerSessionManager.getInstance().createSession(user);

        BigDecimal balance = user instanceof Bidder ? ((Bidder) user).getBalance() : BigDecimal.ZERO;

        send(new LoginResponse("Đăng nhập thành công", sessionId, true, user.getId(), user.getAccount(), user.getRole(), balance));

        System.out.println("User đăng nhập: " + user.getAccount());
    }

    private void handleRegister(RegisterRequest req) {

        if (req.getUserName() == null
                || req.getUserPassword() == null) {
            send(new RegisterResponse("Dữ liệu không hợp lệ", null, false, -1, null));

            return;
        }
        boolean ok = userService.register(req.getUserName(), req.getUserPassword());

        send(new RegisterResponse(
                        ok
                                ? "Đăng ký thành công"
                                : "Tên tài khoản đã tồn tại",
                        null,
                        ok,
                        -1,
                        ok ? req.getUserName() : null
                )
        );
    }

    private void handleBid(BidRequest req) {

        String sessionId =
                req.getSessionId();

        User user =
                ServerSessionManager.getInstance()
                        .getUser(sessionId);

        if (user == null) {

            send(new BidResponse("Chưa đăng nhập", sessionId, false, req.getAuctionId(), BigDecimal.ZERO, null, -1));

            return;
        }

        int auctionId =
                req.getAuctionId();

        BigDecimal amount =
                req.getAmount();

        Auction auction =
                auctionService.getAuctionById(auctionId);

        if (auction == null) {

            send(new BidResponse("Auction không tồn tại", sessionId, false, auctionId, BigDecimal.ZERO, null, -1));

            return;
        }
        if (auction.getStatus() != AuctionStatus.OPEN) {
            send(
                    new BidResponse(
                            "Phiên đấu giá đã đóng",
                            sessionId,
                            false,
                            auctionId,
                            auction.getCurrentPrice(),
                            null,
                            -1
                    )
            );

            return;
        }
        if (amount.compareTo(
                auction.getCurrentPrice()
        ) <= 0) {
            send(
                    new BidResponse(
                            "Giá phải cao hơn "
                                    + auction.getCurrentPrice(),
                            sessionId,
                            false,
                            auctionId,
                            auction.getCurrentPrice(),
                            null,
                            -1
                    )
            );

            return;
        }
        // UPDATE CURRENT PRICE
        boolean auctionUpdated =
                auctionService.placeBid(
                        auctionId,
                        amount
                );

        if (!auctionUpdated) {

            send(
                    new BidResponse(
                            "Đặt giá thất bại",
                            sessionId,
                            false,
                            auctionId,
                            auction.getCurrentPrice(),
                            null,
                            -1
                    )
            );

            return;
        }
        // SAVE BID HISTORY
        boolean bidSaved =
                bidService.placeBid(
                        auctionId,
                        user.getId(),
                        amount
                );
        if (!bidSaved) {

            send(
                    new BidResponse(
                            "Lưu lịch sử bid thất bại",
                            sessionId,
                            false,
                            auctionId,
                            auction.getCurrentPrice(),
                            null,
                            -1
                    )
            );
            return;
        }
        Auction updated =
                auctionService.getAuctionById(
                        auctionId
                );
        BidResponse response =
                new BidResponse(
                        "Đặt giá thành công",
                        sessionId,
                        true,
                        auctionId,
                        updated.getCurrentPrice(),
                        LocalDateTime.now(),
                        user.getId()
                );
        String responseJson =
                gson.toJson(response);
        broadcastAll(responseJson);
        System.out.println(
                "Bid mới: auction="
                        + auctionId
                        + " amount="
                        + amount
        );
    }
    private void handleAddItem(AddItemRequest req) {
        User user =
                ServerSessionManager.getInstance()
                        .getUser(req.getSessionId());

        if (user == null) {
            send(
                    new AddItemResponse(
                            "Chưa đăng nhập",
                            null,
                            false,
                            -1,
                            -1,
                            null
                    )
            );
            return;
        }
        send(
                new AddItemResponse(
                        "Not implemented",
                        req.getSessionId(),
                        false,
                        -1,
                        -1,
                        null
                )
        );
    }
    private void send(Object obj) {
        out.println(
                gson.toJson(obj)
        );
    }
    private void broadcastAll(String json) {
        for (ClientHandler c : clients.values()) {
            c.out.println(json);
        }
    }
}