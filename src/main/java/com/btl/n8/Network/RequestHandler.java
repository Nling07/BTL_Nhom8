package com.btl.n8.Network;

import com.btl.n8.Exception.AuctionClosedException;
import com.btl.n8.Exception.AuthenticationException;
import com.btl.n8.Exception.InvalidBidException;
import com.btl.n8.Connection.*;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Bidder;
import com.btl.n8.Model.Entity.User;
import com.btl.n8.Model.Enums.AuctionStatus;
import com.btl.n8.DTO.*;
import com.btl.n8.Service.*;
import com.google.gson.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import com.btl.n8.Util.LocalDateTimeAdapter;

public class RequestHandler {

    private final ClientHandler clientHandler;
    private final ConcurrentHashMap<String, ClientHandler> clients;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private static final int SNIPE_WINDOW_SECONDS    = 30;
    private static final int SNIPE_EXTENSION_SECONDS = 30;

    private AuctionService auctionService;
    private UserService    userService;
    private BidService     bidService;
    private ItemService    itemService;
    private Connection     conn;

    public RequestHandler(ClientHandler clientHandler,
                          ConcurrentHashMap<String, ClientHandler> clients) {
        this.clientHandler = clientHandler;
        this.clients       = clients;

        try {
            conn           = DataConnection.getConnection();
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
        clientHandler.setLoginSessionId(sessionId);
        // Lấy balance cho cả Bidder lẫn Seller (Seller vẫn có balance sau upgradeToSeller)
        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
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

    public void handleBid(BidRequest req) {
        String sessionId = req.getSessionId();

        try {
            // FIX: luôn lấy user mới nhất từ DB thay vì dùng object cache trong SessionManager
            // để balance check luôn chính xác (tránh dùng balance cũ trước khi bị trừ khi thắng)
            User sessionUser = ServerSessionManager.getInstance().getUser(sessionId);
            if (sessionUser == null) {
                throw new AuthenticationException("Chưa đăng nhập");
            }
            User user = userService.getUserById(sessionUser.getId());
            if (user == null) {
                throw new AuthenticationException("Không tìm thấy user");
            }
            // Cập nhật lại object trong session với dữ liệu mới nhất
            ServerSessionManager.getInstance().refreshUser(sessionId, user);

            int        auctionId = req.getAuctionId();
            BigDecimal amount    = req.getAmount();

            Auction auction = auctionService.getAuctionById(auctionId);
            if (auction == null) {
                throw new InvalidBidException("Auction không tồn tại");
            }

            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(auction.getEndTime())) {
                if (auction.getStatus() == AuctionStatus.OPEN) {
                    settleAuction(auctionId);
                }
                throw new AuctionClosedException("Phiên đấu giá đã kết thúc", auctionId);
            }

            if (auction.getStatus() != AuctionStatus.OPEN) {
                throw new AuctionClosedException("Phiên đấu giá đã đóng", auctionId);
            }

            if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
                throw new InvalidBidException(
                        "Giá phải cao hơn " + auction.getCurrentPrice(),
                        auction.getCurrentPrice().doubleValue(),
                        amount.doubleValue()
                );
            }

            // Balance check dùng user fresh từ DB
            BigDecimal userBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
            if (amount.compareTo(userBalance) > 0) {
                Auction cur = auctionService.getAuctionById(auctionId);
                send(new BidResponse("Số dư không đủ! Số dư hiện tại: "
                        + String.format("%,.0f ₫", userBalance)
                        + " — Cần: " + String.format("%,.0f ₫", amount),
                        sessionId, false, auctionId,
                        cur != null ? cur.getCurrentPrice() : BigDecimal.ZERO,
                        null, -1));
                return;
            }

            // FIX CRITICAL: bọc updateCurrentPrice + insert bid trong 1 transaction.
            // QUAN TRỌNG: Không gọi auctionService.placeBid() hay bidService.placeBid()
            // bên trong transaction vì cả hai đều gọi thêm SELECT (findById / findHighestBid)
            // → dưới REPEATABLE READ, các SELECT này đọc snapshot cũ → trả về false
            // → rollback bid hợp lệ. Thay vào đó gọi thẳng DAO write-only bên trong tx.
            boolean success = false;
            try {
                conn.setAutoCommit(false);

                // 1. Cập nhật current_price của auction (chỉ UPDATE, không SELECT lại)
                boolean auctionUpdated = auctionService.getAuctionDAO()
                        .updateCurrentPrice(auctionId, amount);
                if (!auctionUpdated) {
                    conn.rollback();
                    throw new AuctionClosedException("Đặt giá thất bại — phiên có thể đã kết thúc", auctionId);
                }

                // 2. Mark tất cả bid cũ là OUTBID
                bidService.markAllOutbid(auctionId);

                // 3. Insert bid mới (chỉ write, không SELECT kiểm tra lại)
                com.btl.n8.Model.Entity.Bid newBid = new com.btl.n8.Model.Entity.Bid();
                newBid.setAuctionId(auctionId);
                newBid.setBidderId(user.getId());
                newBid.setAmount(amount);
                newBid.setBidTime(LocalDateTime.now());
                newBid.setStatus(com.btl.n8.Model.Enums.BidStatus.ACTIVE);

                boolean bidSaved = bidService.insertBid(newBid);
                if (!bidSaved) {
                    conn.rollback();
                    throw new InvalidBidException("Lưu lịch sử bid thất bại");
                }

                conn.commit();
                success = true;
            } catch (java.sql.SQLException e) {
                try { conn.rollback(); } catch (java.sql.SQLException ignored) {}
                throw new InvalidBidException("Lỗi DB khi đặt giá: " + e.getMessage());
            } finally {
                try { conn.setAutoCommit(true); } catch (java.sql.SQLException ignored) {}
            }

            if (!success) return;

            // Fetch auction mới nhất SAU transaction để có endTime chính xác
            Auction updated = auctionService.getAuctionById(auctionId);

            // Anti-sniping: dùng endTime từ 'updated' (fetch SAU transaction)
            // KHÔNG dùng auction.getEndTime() vì object đó được fetch TRƯỚC transaction
            // → nếu bid trước đã gia hạn, endTime cũ sẽ tính sai secondsLeft
            LocalDateTime newEndTime = null;
            long secondsLeft = java.time.Duration.between(now, updated.getEndTime()).getSeconds();
            if (secondsLeft <= SNIPE_WINDOW_SECONDS) {
                newEndTime = auctionService.extendEndTime(auctionId, SNIPE_EXTENSION_SECONDS);
                if (newEndTime != null) {
                    System.out.println("Anti-snipe: auction=" + auctionId + " gia hạn đến " + newEndTime);
                }
            }

            BidResponse response = new BidResponse(
                    "Đặt giá thành công", sessionId, true,
                    auctionId, updated.getCurrentPrice(),
                    now, user.getId());

            if (newEndTime != null) {
                response.setNewEndTime(newEndTime);
            }

            broadcastAll(gson.toJson(response));
            System.out.println("Bid mới: auction=" + auctionId + " amount=" + amount
                    + (newEndTime != null ? " [ANTI-SNIPE gia hạn]" : ""));

        } catch (AuthenticationException e) {
            System.err.println("[AuthenticationException] " + e.getMessage());
            send(new BidResponse(e.getMessage(), sessionId,
                    false, req.getAuctionId(), BigDecimal.ZERO, null, -1));

        } catch (AuctionClosedException e) {
            System.err.println("[AuctionClosedException] auctionId=" + e.getAuctionId()
                    + " - " + e.getMessage());
            Auction cur = auctionService.getAuctionById(e.getAuctionId());
            send(new BidResponse(e.getMessage(), sessionId,
                    false, e.getAuctionId(),
                    cur != null ? cur.getCurrentPrice() : BigDecimal.ZERO,
                    null, -1));

        } catch (InvalidBidException e) {
            System.err.println("[InvalidBidException] " + e.getMessage()
                    + (e.getCurrentPrice() >= 0
                    ? " (current=" + e.getCurrentPrice()
                      + ", attempted=" + e.getAttemptedPrice() + ")"
                    : ""));
            Auction cur = auctionService.getAuctionById(req.getAuctionId());
            send(new BidResponse(e.getMessage(), sessionId,
                    false, req.getAuctionId(),
                    cur != null ? cur.getCurrentPrice() : BigDecimal.ZERO,
                    null, -1));
        }
    }

    // ── SETTLE AUCTION ────────────────────────────────────────────────────────

    public void settleAuction(int auctionId) {
        new SettlementHandler(clients).settleAuction(auctionId);
    }

    // ── ADD_ITEM ──────────────────────────────────────────────────────────────

    public void handleAddItem(AddItemRequest req) {
        try {
            User user = ServerSessionManager.getInstance().getUser(req.getSessionId());
            if (user == null) {
                throw new AuthenticationException("Chưa đăng nhập");
            }
            send(new AddItemResponse("Not implemented", req.getSessionId(), false, -1, -1, null));
        } catch (AuthenticationException e) {
            System.err.println("[AuthenticationException] " + e.getMessage());
            send(new AddItemResponse(e.getMessage(), null, false, -1, -1, null));
        }
    }

    // ── AUTO_BID ──────────────────────────────────────────────────────────────

    public void handleAutoBid(AutoBidRequest req) {
        try {
            User user = ServerSessionManager.getInstance().getUser(req.getSessionId());
            if (user == null) {
                throw new AuthenticationException("Not authenticated");
            }
            Auction auction = auctionService.getAuctionById(req.getAuctionId());
            if (auction == null || auction.getStatus() != AuctionStatus.OPEN) {
                throw new AuctionClosedException("Auction not open", req.getAuctionId());
            }
            send(new AutoBidResponse("AutoBid registered", req.getSessionId(),
                    true, req.getAuctionId(), req.getMaxPrice(), req.getStep(),
                    false, 0, true));
            System.out.println("AutoBid: user=" + user.getAccount()
                    + " auction=" + req.getAuctionId()
                    + " maxPrice=" + req.getMaxPrice());

        } catch (AuthenticationException e) {
            System.err.println("[AuthenticationException] " + e.getMessage());
            send(new AutoBidResponse(e.getMessage(), req.getSessionId(),
                    false, req.getAuctionId(), null, null, false, 0, false));

        } catch (AuctionClosedException e) {
            System.err.println("[AuctionClosedException] auctionId=" + e.getAuctionId()
                    + " - " + e.getMessage());
            send(new AutoBidResponse(e.getMessage(), req.getSessionId(),
                    false, req.getAuctionId(), req.getMaxPrice(), req.getStep(),
                    false, 0, false));
        }
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