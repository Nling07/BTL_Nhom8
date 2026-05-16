package com.btl.n8.network;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.entity.Auction;
import com.btl.n8.Model.entity.Bidder;
import com.btl.n8.Model.entity.User;
import com.btl.n8.Model.enums.AuctionStatus;
import com.btl.n8.dto.*;
import com.btl.n8.Service.*;
import com.google.gson.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import com.btl.n8.util.LocalDateTimeAdapter;

public class RequestHandler {

    private final ClientHandler clientHandler;
    private final ConcurrentHashMap<String, ClientHandler> clients;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private AuctionService auctionService;
    private UserService    userService;
    private BidService     bidService;
    private ItemService    itemService;

    public RequestHandler(ClientHandler clientHandler,
                          ConcurrentHashMap<String, ClientHandler> clients) {
        this.clientHandler = clientHandler;
        this.clients       = clients;

        try {
            Connection conn = DataConnection.getConnection();
            userService    = new UserService(new UserDAOImpl(conn));
            bidService     = new BidService(new BidDAOImpl(conn));
            itemService    = new ItemService(new ItemDAOImpl(conn));
            auctionService = new AuctionService(new AuctionDAOImpl(conn));
        } catch (Exception e) {
            System.err.println("Lỗi khởi tạo Service: " + e.getMessage());
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    public void handleLogin(LoginRequest req) {
        User user = userService.login(req.getUsername(), req.getPassword());
        if (user == null) {
            send(new LoginResponse("Sai tài khoản hoặc mật khẩu", null, false, -1, null, null, null));
            return;
        }
        String sessionId = ServerSessionManager.getInstance().createSession(user);
        BigDecimal balance = user instanceof Bidder ? ((Bidder) user).getBalance() : BigDecimal.ZERO;
        send(new LoginResponse("Đăng nhập thành công", sessionId, true,
                user.getId(), user.getAccount(), user.getRole(), balance));
        System.out.println("User đăng nhập: " + user.getAccount());
    }

    // ── REGISTER ──────────────────────────────────────────────────────────────

    public void handleRegister(RegisterRequest req) {
        if (req.getUserName() == null || req.getUserPassword() == null) {
            send(new RegisterResponse("Dữ liệu không hợp lệ", null, false, -1, null));
            return;
        }
        boolean ok = userService.register(req.getUserName(), req.getUserPassword());
        send(new RegisterResponse(
                ok ? "Đăng ký thành công" : "Tên tài khoản đã tồn tại",
                null, ok, -1, ok ? req.getUserName() : null));
    }

    // ── BID ───────────────────────────────────────────────────────────────────

    /**
     * FIX BUG 2: Mọi nhánh thất bại đều gửi BidResponse về client.
     * Trước đây một số nhánh (endTime qua, placeBid trả false) trả về im lặng
     * → client mãi hiện "Bid sent..." không load được giá mới.
     */
    public void handleBid(BidRequest req) {
        String sessionId = req.getSessionId();
        User user = ServerSessionManager.getInstance().getUser(sessionId);

        if (user == null) {
            send(new BidResponse("Chưa đăng nhập", sessionId,
                    false, req.getAuctionId(), BigDecimal.ZERO, null, -1));
            return;
        }

        int        auctionId = req.getAuctionId();
        BigDecimal amount    = req.getAmount();

        Auction auction = auctionService.getAuctionById(auctionId);

        if (auction == null) {
            send(new BidResponse("Auction không tồn tại", sessionId,
                    false, auctionId, BigDecimal.ZERO, null, -1));
            return;
        }

        // FIX BUG 2a: check endTime trước — nếu hết giờ trả lỗi rõ ràng
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            // cũng tự đóng auction trong DB luôn
            if (auction.getStatus() == AuctionStatus.OPEN) {
                auctionService.closeAuction(auctionId);
            }
            send(new BidResponse("Phiên đấu giá đã kết thúc", sessionId,
                    false, auctionId, auction.getCurrentPrice(), null, -1));
            return;
        }

        if (auction.getStatus() != AuctionStatus.OPEN) {
            send(new BidResponse("Phiên đấu giá đã đóng", sessionId,
                    false, auctionId, auction.getCurrentPrice(), null, -1));
            return;
        }

        if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
            send(new BidResponse("Giá phải cao hơn " + auction.getCurrentPrice(), sessionId,
                    false, auctionId, auction.getCurrentPrice(), null, -1));
            return;
        }

        // Update giá auction
        boolean auctionUpdated = auctionService.placeBid(auctionId, amount);
        if (!auctionUpdated) {
            // FIX BUG 2b: placeBid trả false (có thể do race condition endTime)
            // → phải gửi response về không thì client đơ
            Auction latest = auctionService.getAuctionById(auctionId);
            send(new BidResponse("Đặt giá thất bại — phiên có thể đã kết thúc", sessionId,
                    false, auctionId,
                    latest != null ? latest.getCurrentPrice() : auction.getCurrentPrice(),
                    null, -1));
            return;
        }

        // Lưu lịch sử bid
        boolean bidSaved = bidService.placeBid(auctionId, user.getId(), amount);
        if (!bidSaved) {
            send(new BidResponse("Lưu lịch sử bid thất bại", sessionId,
                    false, auctionId, auction.getCurrentPrice(), null, -1));
            return;
        }

        Auction updated = auctionService.getAuctionById(auctionId);
        BidResponse response = new BidResponse(
                "Đặt giá thành công", sessionId, true,
                auctionId, updated.getCurrentPrice(),
                LocalDateTime.now(), user.getId());

        broadcastAll(gson.toJson(response));
        System.out.println("Bid mới: auction=" + auctionId + " amount=" + amount);
    }

    // ── ADD_ITEM ──────────────────────────────────────────────────────────────

    public void handleAddItem(AddItemRequest req) {
        User user = ServerSessionManager.getInstance().getUser(req.getSessionId());
        if (user == null) {
            send(new AddItemResponse("Chưa đăng nhập", null, false, -1, -1, null));
            return;
        }
        send(new AddItemResponse("Not implemented", req.getSessionId(), false, -1, -1, null));
    }

    // ── AUTO_BID ──────────────────────────────────────────────────────────────

    public void handleAutoBid(AutoBidRequest req) {
        User user = ServerSessionManager.getInstance().getUser(req.getSessionId());
        if (user == null) {
            send(new AutoBidResponse("Not authenticated", req.getSessionId(),
                    false, req.getAuctionId(), null, null, false, 0, false));
            return;
        }
        Auction auction = auctionService.getAuctionById(req.getAuctionId());
        if (auction == null || auction.getStatus() != AuctionStatus.OPEN) {
            send(new AutoBidResponse("Auction not open", req.getSessionId(),
                    false, req.getAuctionId(), req.getMaxPrice(), req.getStep(),
                    req.isSnipeMode(), req.getSnipeSeconds(), false));
            return;
        }
        send(new AutoBidResponse("AutoBid registered", req.getSessionId(),
                true, req.getAuctionId(), req.getMaxPrice(), req.getStep(),
                req.isSnipeMode(), req.getSnipeSeconds(), true));
        System.out.println("AutoBid: user=" + user.getAccount()
                + " auction=" + req.getAuctionId()
                + " maxPrice=" + req.getMaxPrice()
                + " snipe=" + req.isSnipeMode());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void send(Object obj) {
        clientHandler.send(gson.toJson(obj));
    }

    private void broadcastAll(String json) {
        for (ClientHandler c : clients.values()) {
            c.send(json);
        }
    }
}