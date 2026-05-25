package com.btl.n8.Network;

import com.btl.n8.Exception.AuctionClosedException;
import com.btl.n8.Exception.AuthenticationException;
import com.btl.n8.Exception.InvalidBidException;
import com.btl.n8.Connection.*;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Bid;
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

/**
 * Xử lý tất cả request từ client.
 *
 * Dependency Injection: không tự new DAO nữa — dùng ServiceFactory.
 * Frozen Balance: freeze trước khi bid, unfreeze người bị outbid, settle khi thắng.
 * Concurrent Bid: lock per-auctionId ở BidService đảm bảo chỉ 1 thread xử lý
 *   1 auction tại 1 thời điểm → tránh 2 người cùng thắng với cùng 1 giá.
 */
public class RequestHandler {

    private final ClientHandler clientHandler;
    private final ConcurrentHashMap<String, ClientHandler> clients;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private static final int SNIPE_WINDOW_SECONDS    = 30;
    private static final int SNIPE_EXTENSION_SECONDS = 30;

    // Inject qua ServiceFactory — không còn tự new DAO trực tiếp
    private final AuctionService auctionService;
    private final UserService    userService;
    private final BidService     bidService;
    private final ItemService    itemService;
    private final Connection     conn;

    public RequestHandler(ClientHandler clientHandler,
                          ConcurrentHashMap<String, ClientHandler> clients) {
        this.clientHandler = clientHandler;
        this.clients       = clients;

        Connection tmpConn = null;
        AuctionService tmpAuction = null;
        UserService    tmpUser    = null;
        BidService     tmpBid     = null;
        ItemService    tmpItem    = null;

        try {
            tmpConn   = DataConnection.getConnection();
            // Dùng ServiceFactory thay vì new trực tiếp
            tmpUser    = ServiceFactory.createUserService(tmpConn);
            tmpBid     = ServiceFactory.createBidService(tmpConn);
            tmpItem    = ServiceFactory.createItemService(tmpConn);
            tmpAuction = ServiceFactory.createAuctionService(tmpConn);
        } catch (Exception e) {
            System.err.println("Lỗi khởi tạo Service: " + e.getMessage());
        }

        this.conn           = tmpConn;
        this.userService    = tmpUser;
        this.bidService     = tmpBid;
        this.itemService    = tmpItem;
        this.auctionService = tmpAuction;
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
            // Luôn lấy user mới nhất từ DB để balance/frozenBalance chính xác
            User sessionUser = ServerSessionManager.getInstance().getUser(sessionId);
            if (sessionUser == null) throw new AuthenticationException("Chưa đăng nhập");

            User user = userService.getUserById(sessionUser.getId());
            if (user == null) throw new AuthenticationException("Không tìm thấy user");
            ServerSessionManager.getInstance().refreshUser(sessionId, user);

            int        auctionId = req.getAuctionId();
            BigDecimal amount    = req.getAmount();

            Auction auction = auctionService.getAuctionById(auctionId);
            if (auction == null) throw new InvalidBidException("Auction không tồn tại");

            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(auction.getEndTime())) {
                if (auction.getStatus() == AuctionStatus.OPEN) settleAuction(auctionId);
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

            // ── Frozen balance check ──────────────────────────────────────────
            // Dùng getAvailableBalance() = balance - frozenBalance
            // thay vì getBalance() để tránh double-spend khi đấu giá nhiều item
            BigDecimal available = user.getAvailableBalance();
            if (amount.compareTo(available) > 0) {
                Auction cur = auctionService.getAuctionById(auctionId);
                send(new BidResponse(
                        "Số dư khả dụng không đủ! Khả dụng: "
                                + String.format("%,.0f ₫", available)
                                + " (tổng: " + String.format("%,.0f ₫", user.getBalance())
                                + ", đang khóa: " + String.format("%,.0f ₫", user.getFrozenBalance()) + ")"
                                + " — Cần: " + String.format("%,.0f ₫", amount),
                        sessionId, false, auctionId,
                        cur != null ? cur.getCurrentPrice() : BigDecimal.ZERO,
                        null, -1));
                return;
            }

            // ── FREEZE trước khi bid ──────────────────────────────────────────
            // Atomic tại DB: chỉ freeze nếu available >= amount (tránh race condition
            // khi 2 request đến cùng lúc với cùng user)
            boolean frozen = userService.freezeBalance(user.getId(), amount);
            if (!frozen) {
                Auction cur = auctionService.getAuctionById(auctionId);
                send(new BidResponse(
                        "Số dư khả dụng không đủ (kiểm tra đồng thời thất bại)",
                        sessionId, false, auctionId,
                        cur != null ? cur.getCurrentPrice() : BigDecimal.ZERO,
                        null, -1));
                return;
            }

            // ── Transaction: updatePrice + markOutbid + insertBid ─────────────
            boolean success = false;
            Bid previousActiveBid = null; // bid cũ ACTIVE của user này (nếu có) — để unfreeze

            try {
                conn.setAutoCommit(false);

                // Tìm bid ACTIVE hiện tại của chính user này trong auction này
                // để sau này unfreeze đúng số tiền
                previousActiveBid = bidService.getActiveBidByBidder(auctionId, user.getId());

                // 1. Cập nhật current_price
                boolean auctionUpdated = auctionService.getAuctionDAO()
                        .updateCurrentPrice(auctionId, amount);
                if (!auctionUpdated) {
                    conn.rollback();
                    // Hoàn lại frozen vừa lock
                    userService.unfreezeBalance(user.getId(), amount);
                    throw new AuctionClosedException("Đặt giá thất bại — phiên có thể đã kết thúc", auctionId);
                }

                // 2. Mark tất cả bid cũ là OUTBID
                bidService.markAllOutbid(auctionId);

                // 3. Insert bid mới
                com.btl.n8.Model.Entity.Bid newBid = new com.btl.n8.Model.Entity.Bid();
                newBid.setAuctionId(auctionId);
                newBid.setBidderId(user.getId());
                newBid.setAmount(amount);
                newBid.setBidTime(now);
                newBid.setStatus(com.btl.n8.Model.Enums.BidStatus.ACTIVE);

                boolean bidSaved = bidService.insertBid(newBid);
                if (!bidSaved) {
                    conn.rollback();
                    userService.unfreezeBalance(user.getId(), amount);
                    throw new InvalidBidException("Lưu lịch sử bid thất bại");
                }

                conn.commit();
                success = true;

            } catch (java.sql.SQLException e) {
                try { conn.rollback(); } catch (java.sql.SQLException ignored) {}
                userService.unfreezeBalance(user.getId(), amount);
                throw new InvalidBidException("Lỗi DB khi đặt giá: " + e.getMessage());
            } finally {
                try { conn.setAutoCommit(true); } catch (java.sql.SQLException ignored) {}
            }

            if (!success) return;

            // ── Unfreeze tiền của những người bị outbid ──────────────────────
            // Broadcast danh sách bidder bị outbid để server unfreeze cho họ.
            // Ta query bids vừa bị mark OUTBID trong cùng auction này (trừ chính user hiện tại).
            // Đơn giản nhất: unfreeze previousActiveBid của chính user (nếu user nâng giá chính mình)
            if (previousActiveBid != null) {
                // User này đang nâng giá từ oldAmount lên amount → chỉ freeze thêm diff,
                // nhưng ta đã freeze toàn bộ amount → cần unfreeze phần cũ
                userService.unfreezeBalance(user.getId(), previousActiveBid.getAmount());
            }

            // Unfreeze cho tất cả bidder khác vừa bị outbid trong auction này
            unfreezeOutbidders(auctionId, user.getId(), bidService);

            // ── Anti-sniping ──────────────────────────────────────────────────
            Auction updated = auctionService.getAuctionById(auctionId);
            LocalDateTime newEndTime = null;
            long secondsLeft = java.time.Duration.between(now, updated.getEndTime()).getSeconds();
            if (secondsLeft <= SNIPE_WINDOW_SECONDS) {
                newEndTime = auctionService.extendEndTime(auctionId, SNIPE_EXTENSION_SECONDS);
                if (newEndTime != null)
                    System.out.println("Anti-snipe: auction=" + auctionId + " gia hạn đến " + newEndTime);
            }

            BidResponse response = new BidResponse(
                    "Đặt giá thành công", sessionId, true,
                    auctionId, updated.getCurrentPrice(), now, user.getId());
            if (newEndTime != null) response.setNewEndTime(newEndTime);

            broadcastAll(gson.toJson(response));
            System.out.println("Bid mới: auction=" + auctionId + " user=" + user.getAccount()
                    + " amount=" + amount
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
            System.err.println("[InvalidBidException] " + e.getMessage());
            Auction cur = auctionService.getAuctionById(req.getAuctionId());
            send(new BidResponse(e.getMessage(), sessionId,
                    false, req.getAuctionId(),
                    cur != null ? cur.getCurrentPrice() : BigDecimal.ZERO,
                    null, -1));
        }
    }

    /**
     * Unfreeze cho tất cả bidder bị outbid (trừ chính người vừa bid).
     * Gọi sau khi transaction commit thành công.
     */
    private void unfreezeOutbidders(int auctionId, int currentBidderId, BidService bidService) {
        // Lấy danh sách bid OUTBID vừa bị mark — query bid cũ ACTIVE của người khác
        // Đơn giản: lấy tất cả bid OUTBID của auction, trừ của currentBidder
        // Mỗi bid OUTBID → unfreeze đúng amount của bid đó cho bidder đó
        try {
            java.util.List<com.btl.n8.Model.Entity.Bid> outbidList =
                    bidService.getBidsByAuction(auctionId);
            for (com.btl.n8.Model.Entity.Bid b : outbidList) {
                if (b.getBidderId() == currentBidderId) continue;
                if (b.getStatus() == com.btl.n8.Model.Enums.BidStatus.OUTBID) {
                    // Chỉ unfreeze bid OUTBID vừa mới (status mới đổi)
                    // Để tránh unfreeze nhiều lần, chỉ unfreeze bid OUTBID trong lần này
                    // thực tế nên thêm flag "frozen_released" nhưng để đơn giản cho BTL:
                    // chỉ unfreeze bid cao nhất của mỗi bidder (bid gần nhất)
                }
            }
            // Cách đơn giản hơn cho BTL: unfreeze bid active vừa bị outbid của từng người
            // Gọi thẳng unfreezeForOutbid trong BidService
            bidService.unfreezeOutbidBalances(auctionId, currentBidderId, userService);
        } catch (Exception e) {
            System.err.println("[RequestHandler] Lỗi unfreeze outbidders: " + e.getMessage());
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
            if (user == null) throw new AuthenticationException("Chưa đăng nhập");
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
            if (user == null) throw new AuthenticationException("Not authenticated");

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
            System.err.println("[AuctionClosedException] auctionId=" + e.getAuctionId());
            send(new AutoBidResponse(e.getMessage(), req.getSessionId(),
                    false, req.getAuctionId(), req.getMaxPrice(), req.getStep(),
                    false, 0, false));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void send(Object obj)         { clientHandler.send(gson.toJson(obj)); }
    private void broadcastAll(String json) {
        for (ClientHandler c : clients.values()) c.send(json);
    }
}