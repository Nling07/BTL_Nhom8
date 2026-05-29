package com.btl.n8.Network;

import com.btl.n8.Exception.AuctionClosedException;
import com.btl.n8.Exception.AuthenticationException;
import com.btl.n8.Exception.InvalidBidException;
import com.btl.n8.Connection.*;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Bid;
import com.btl.n8.Model.Entity.Item;
import com.btl.n8.Model.Entity.User;
import com.btl.n8.Model.Enums.AuctionStatus;
import com.btl.n8.DTO.*;
import com.btl.n8.Service.*;
import com.google.gson.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import com.btl.n8.Util.LocalDateTimeAdapter;

public class RequestHandler {

    private final ClientHandler clientHandler;
    private final ConcurrentHashMap<String, ClientHandler> clients;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private static final int SNIPE_WINDOW_SECONDS    = 30;
    private static final int SNIPE_EXTENSION_SECONDS = 30;

    // FIX concurrent: lock per-auctionId dùng chung toàn server (static)
    // Đảm bảo chỉ 1 request bid vào 1 auction tại 1 thời điểm
    private static final ConcurrentHashMap<Integer, ReentrantLock> bidLocks =
            new ConcurrentHashMap<>();

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
            tmpConn    = DataConnection.getConnection();
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

    private ReentrantLock getBidLock(int auctionId) {
        return bidLocks.computeIfAbsent(auctionId, id -> new ReentrantLock(true));
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

        ReentrantLock lock = getBidLock(req.getAuctionId());
        lock.lock();
        try {
            _handleBidLocked(req, sessionId);
        } catch (Exception e) {
            // Bắt mọi exception không mong đợi (SQLException từ broken connection, v.v.)
            // để tránh crash ClientHandler → client bị mất kết nối
            System.err.println("[BID ERROR] Unexpected exception: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            try {
                send(new BidResponse("Lỗi hệ thống, vui lòng thử lại",
                        sessionId, false, req.getAuctionId(), BigDecimal.ZERO, null, -1));
            } catch (Exception ignored) {}
        } finally {
            lock.unlock();
        }
    }

    /**
     * Xử lý bid sau khi đã giữ lock của auction.
     * Tách ra method riêng cho dễ đọc.
     */
    private void _handleBidLocked(BidRequest req, String sessionId) {
        try {
            // Luôn lấy user mới nhất từ DB để balance/frozenBalance chính xác
            User sessionUser = ServerSessionManager.getInstance().getUser(sessionId);
            if (sessionUser == null) throw new AuthenticationException("Chưa đăng nhập");

            User user = userService.getUserById(sessionUser.getId());
            if (user == null) throw new AuthenticationException("Không tìm thấy user");
            ServerSessionManager.getInstance().refreshUser(sessionId, user);

            // ═══════════════════════════════════════════════════════
            // [DEBUG] In trạng thái user + request ngay khi vào bid
            System.out.println("\n[DEBUG-BID] ══════════════════════════════════");
            System.out.println("[DEBUG-BID] user      = " + user.getAccount() + " (id=" + user.getId() + ")");
            System.out.println("[DEBUG-BID] auctionId = " + req.getAuctionId());
            System.out.println("[DEBUG-BID] amount    = " + req.getAmount());
            System.out.println("[DEBUG-BID] balance        = " + user.getBalance());
            System.out.println("[DEBUG-BID] frozenBalance  = " + user.getFrozenBalance());
            System.out.println("[DEBUG-BID] availableBalance = " + user.getAvailableBalance());

            // ═══════════════════════════════════════════════════════

            int        auctionId = req.getAuctionId();
            BigDecimal amount    = req.getAmount();

            // Fetch auction mới nhất — QUAN TRỌNG: phải fetch SAU khi giữ lock
            // để đọc currentPrice chính xác (người trước có thể vừa cập nhật)
            Auction auction = auctionService.getAuctionById(auctionId);
            if (auction == null) throw new InvalidBidException("Auction không tồn tại");

            // FIX anti-sniping: lấy now SAU khi giữ lock để tính secondsLeft chính xác
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

            // ── FREEZE: chỉ freeze phần CHÊNH LỆCH so với bid cũ ───────────
            // Nếu user đang là highest bidder auction này và muốn nâng giá,
            // phần cũ đã bị frozen rồi → chỉ cần freeze thêm phần chênh lệch.
            // Tránh trường hợp: balance=200, frozen=100(bidX), bid thêm 120 cho itemY
            // → available=100, đủ 120? Không. Đúng.
            // Nhưng nếu nâng giá itemX từ 100→150: available=100, cần 150? Sai!
            // Thực tế chỉ cần freeze thêm 50 (=150-100).
            Bid existingBidInThisAuction = bidService.getActiveBidByBidder(auctionId, user.getId());
            BigDecimal alreadyFrozenForThis = existingBidInThisAuction != null
                    ? existingBidInThisAuction.getAmount() : BigDecimal.ZERO;
            BigDecimal extraToFreeze = amount.subtract(alreadyFrozenForThis);

            // Kiểm tra available có đủ để freeze thêm không
            if (extraToFreeze.compareTo(BigDecimal.ZERO) > 0
                    && extraToFreeze.compareTo(user.getAvailableBalance()) > 0) {
                Auction cur = auctionService.getAuctionById(auctionId);
                send(new BidResponse(
                        "Số dư khả dụng không đủ! Khả dụng: "
                                + String.format("%,.0f ₫", user.getAvailableBalance())
                                + " — Cần thêm: " + String.format("%,.0f ₫", extraToFreeze),
                        sessionId, false, auctionId,
                        cur != null ? cur.getCurrentPrice() : BigDecimal.ZERO,
                        null, -1));
                return;
            }

            boolean frozen = true;
            if (extraToFreeze.compareTo(BigDecimal.ZERO) > 0) {
                frozen = userService.freezeBalance(user.getId(), extraToFreeze);
            }
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
            Bid previousActiveBid = null;

            try {
                System.out.println("[DEBUG-TXN] setAutoCommit(false)");
                conn.setAutoCommit(false);

                // Lấy bid ACTIVE cũ của chính user này trong auction (nếu đang tự nâng giá)
                previousActiveBid = bidService.getActiveBidByBidder(auctionId, user.getId());
                System.out.println("[DEBUG-TXN] previousActiveBid = "
                        + (previousActiveBid != null ? previousActiveBid.getAmount() : "NONE"));

                // 1. Cập nhật current_price
                boolean auctionUpdated = auctionService.updateCurrentPrice(auctionId, amount);
                System.out.println("[DEBUG-TXN] updateCurrentPrice → " + auctionUpdated);
                if (!auctionUpdated) {
                    conn.rollback();
                    userService.unfreezeBalance(user.getId(), amount);
                    throw new AuctionClosedException("Đặt giá thất bại — phiên có thể đã kết thúc", auctionId);
                }

                // 2. Mark tất cả bid cũ là OUTBID
                bidService.markAllOutbid(auctionId);
                System.out.println("[DEBUG-TXN] markAllOutbid done");

                // 3. Insert bid mới
                Bid newBid = new Bid();
                newBid.setAuctionId(auctionId);
                newBid.setBidderId(user.getId());
                newBid.setAmount(amount);
                newBid.setBidTime(now);
                newBid.setStatus(com.btl.n8.Model.Enums.BidStatus.ACTIVE);

                boolean bidSaved = bidService.insertBid(newBid);
                System.out.println("[DEBUG-TXN] insertBid → " + bidSaved);
                if (!bidSaved) {
                    conn.rollback();
                    userService.unfreezeBalance(user.getId(), amount);
                    throw new InvalidBidException("Lưu lịch sử bid thất bại");
                }

                conn.commit();
                System.out.println("[DEBUG-TXN] commit OK");
                success = true;

            } catch (java.sql.SQLException e) {
                System.err.println("[DEBUG-TXN] *** SQLException trong transaction: " + e.getMessage());
                System.err.println("[DEBUG-TXN] SQLState=" + e.getSQLState() + " ErrorCode=" + e.getErrorCode());
                try { conn.rollback(); } catch (java.sql.SQLException ignored) {}
                userService.unfreezeBalance(user.getId(), amount);
                throw new InvalidBidException("Lỗi DB khi đặt giá: " + e.getMessage());
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (java.sql.SQLException e) {
                    System.err.println("[DEBUG-TXN] *** setAutoCommit(true) THẤT BẠI: " + e.getMessage());
                }
            }

            if (!success) return;

            // ── Unfreeze sau khi transaction commit thành công ────────────────
            // Không cần unfreeze previousActiveBid vì đã chỉ freeze phần chênh lệch ở trên.
            // Unfreeze cho tất cả bidder khác vừa bị outbid.
            bidService.unfreezeOutbidBalances(auctionId, user.getId(), userService);

            // ── Anti-sniping ──────────────────────────────────────────────────
            // FIX: fetch lại auction SAU transaction để có endTime chính xác
            // rồi tính secondsLeft từ now (đã lấy sau lock) → không bị lệch
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

    // ── SETTLE AUCTION ────────────────────────────────────────────────────────

    public void settleAuction(int auctionId) {
        new SettlementHandler(clients).settleAuction(auctionId);
    }

    // ── ADD_ITEM ──────────────────────────────────────────────────────────────

    public void handleAddItem(AddItemRequest req) {
        try {
            User user = ServerSessionManager.getInstance().getUser(req.getSessionId());
            if (user == null) throw new AuthenticationException("Chưa đăng nhập");

            byte[] imageBytes = req.getImageBase64() != null
                    ? Base64.getDecoder().decode(req.getImageBase64()) : null;

            Item item = itemService.createItem(
                    req.getName(), req.getType().name(),
                    req.getSellerId(), imageBytes);
            boolean itemOk = itemService.addItem(item);
            if (!itemOk) { send(new AddItemResponse("Tạo item thất bại", req.getSessionId(), false, -1, -1, null)); return; }

            Auction auction = new Auction(0, item.getId(),
                    req.getStartingPrice(), req.getStartingPrice(),
                    req.getStartTime(), req.getEndTime(), AuctionStatus.OPEN);
            boolean auctionOk = auctionService.createAuction(auction);

            send(new AddItemResponse(
                    auctionOk ? "Đăng bán thành công" : "Tạo đấu giá thất bại",
                    req.getSessionId(), auctionOk,
                    auction.getId(), item.getId(), req.getEndTime()));
        } catch (AuthenticationException e) {
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

    private void send(Object obj)          { clientHandler.send(gson.toJson(obj)); }
    private void broadcastAll(String json) {
        System.out.println("[BROADCAST] tới " + clients.size() + " clients: " + json.substring(0, Math.min(80, json.length())));
        for (ClientHandler c : clients.values()) c.send(json);
    }

    // ── DEPOSIT ───────────────────────────────────────────────────────────────────
    public void handleDeposit(DepositRequest req) {
        try {
            User user = ServerSessionManager.getInstance().getUser(req.getSessionId());
            if (user == null) throw new AuthenticationException("Chưa đăng nhập");

            BigDecimal current = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
            BigDecimal newBal  = current.add(req.getAmount());
            user.setBalance(newBal);
            boolean ok = userService.updateUser(user);
            send(new DepositResponse(
                    ok ? "Nạp tiền thành công" : "Nạp tiền thất bại",
                    req.getSessionId(), ok, ok ? newBal : current));
        } catch (AuthenticationException e) {
            send(new DepositResponse(e.getMessage(), null, false, BigDecimal.ZERO));
        }
    }

    // ── UPGRADE_SELLER ────────────────────────────────────────────────────────────
    public void handleUpgradeSeller(UpgradeSellerRequest req) {
        try {
            User user = ServerSessionManager.getInstance().getUser(req.getSessionId());
            if (user == null) throw new AuthenticationException("Chưa đăng nhập");

            boolean ok = userService.upgradeToSeller(req.getUserId());
            User updated = ok ? userService.getUserById(req.getUserId()) : null;
            if (ok && updated != null)
                ServerSessionManager.getInstance().updateUser(req.getSessionId(), updated);

            send(new UpgradeSellerResponse(
                    ok ? "Nâng cấp thành công" : "Nâng cấp thất bại",
                    req.getSessionId(), ok, updated));
        } catch (AuthenticationException e) {
            send(new UpgradeSellerResponse(e.getMessage(), null, false, null));
        }
    }

    // ── GET_USER_BIDS ─────────────────────────────────────────────────────────────
    public void handleGetUserBids(GetUserBidsRequest req) {
        try {
            User user = ServerSessionManager.getInstance().getUser(req.getSessionId());
            if (user == null) throw new AuthenticationException("Chưa đăng nhập");

            List<Bid> bids = bidService.getBidsByBidder(req.getUserId());
            send(new GetUserBidsResponse("OK", req.getSessionId(), true, bids));
        } catch (AuthenticationException e) {
            send(new GetUserBidsResponse(e.getMessage(), null, false, null));
        }
    }

    // ── GET_SELLER_ITEMS ──────────────────────────────────────────────────────────
    public void handleGetSellerItems(GetSellerItemsRequest req) {
        try {
            User user = ServerSessionManager.getInstance().getUser(req.getSessionId());
            if (user == null) throw new AuthenticationException("Chưa đăng nhập");

            List<Item>    items    = itemService.getItemsBySeller(req.getSellerId());
            List<Auction> auctions = items.stream()
                    .map(item -> auctionService.getAuctionByItemId(item.getId()))
                    .filter(a -> a != null)
                    .collect(java.util.stream.Collectors.toList());

            send(new GetSellerItemsResponse("OK", req.getSessionId(), true, items, auctions));
        } catch (AuthenticationException e) {
            send(new GetSellerItemsResponse(e.getMessage(), null, false, null, null));
        }
    }

    // ── GET_AUCTION_LIST ──────────────────────────────────────────────────────────
    public void handleGetAuctionList(GetAuctionListRequest req) {
        try {
            User user = ServerSessionManager.getInstance().getUser(req.getSessionId());
            if (user == null) throw new AuthenticationException("Chưa đăng nhập");

            // Đóng các auction hết giờ và lấy danh sách qua AuctionService (không gọi DAO trực tiếp)
            List<com.btl.n8.DTO.ItemAuctionRow> joined = auctionService.getAuctionList();

            List<GetAuctionListResponse.AuctionRow> rows = joined.stream()
                    .map(r -> new GetAuctionListResponse.AuctionRow(
                            r.itemId, r.itemName, r.itemType,
                            r.currentPrice, r.status, r.auctionId, r.sellerId))
                    .collect(java.util.stream.Collectors.toList());

            send(new GetAuctionListResponse("OK", req.getSessionId(), true, rows));
        } catch (AuthenticationException e) {
            send(new GetAuctionListResponse(e.getMessage(), null, false, null));
        }
    }

    // ── GET_AUCTION_DETAIL ────────────────────────────────────────────────────────
    public void handleGetAuctionDetail(GetAuctionDetailRequest req) {
        try {
            User user = ServerSessionManager.getInstance().getUser(req.getSessionId());
            if (user == null) throw new AuthenticationException("Chưa đăng nhập");

            Auction auction  = auctionService.getAuctionById(req.getAuctionId());
            List<Bid> bids   = bidService.getBidsByAuction(req.getAuctionId());

            send(new GetAuctionDetailResponse("OK", req.getSessionId(),
                    auction != null, auction, bids));
        } catch (AuthenticationException e) {
            send(new GetAuctionDetailResponse(e.getMessage(), null, false, null, null));
        }
    }

    // ── ADMIN_* ───────────────────────────────────────────────────────────────────
    public void handleAdmin(AdminRequest req) {
        try {
            User user = ServerSessionManager.getInstance().getUser(req.getSessionId());
            if (user == null) throw new AuthenticationException("Chưa đăng nhập");
            if (user.getRole() != com.btl.n8.Model.Enums.Role.ADMIN)
                throw new AuthenticationException("Không có quyền Admin");

            AdminService adminService = ServiceFactory
                    .createAdminService(conn);
            String sid = req.getSessionId();

            switch (req.getAction()) {
                case "ADMIN_GET_DASHBOARD" -> send(new AdminResponse(
                        "ADMIN_RESULT", "OK", sid, true).withDashboard(
                        adminService.getTotalUsers(), adminService.getTotalAuctions(),
                        adminService.getOpenAuctionCount(), adminService.getTotalItems(),
                        adminService.getTotalSellers(), adminService.getCancelledAuctionCount()));

                case "ADMIN_GET_USERS"     -> send(new AdminResponse(
                        "ADMIN_RESULT", "OK", sid, true)
                        .withUsers(adminService.getAllUsers()));

                case "ADMIN_GET_AUCTIONS"  -> send(new AdminResponse(
                        "ADMIN_RESULT", "OK", sid, true)
                        .withAuctions(adminService.getAllAuctions()));

                case "ADMIN_GET_ITEMS"     -> send(new AdminResponse(
                        "ADMIN_RESULT", "OK", sid, true)
                        .withItems(adminService.getAllItems()));

                case "ADMIN_UPGRADE_USER"  -> { adminService.upgradeUserToSeller(req.getTargetId());
                    send(new AdminResponse("ADMIN_RESULT", "OK", sid, true)); }

                case "ADMIN_DEMOTE_USER"   -> { adminService.demoteSellerToBidder(req.getTargetId());
                    send(new AdminResponse("ADMIN_RESULT", "OK", sid, true)); }

                case "ADMIN_DELETE_USER"   -> { adminService.deleteUserById(req.getTargetId());
                    send(new AdminResponse("ADMIN_RESULT", "OK", sid, true)); }

                case "ADMIN_CLOSE_AUCTION" -> { adminService.closeAuction(req.getTargetId());
                    send(new AdminResponse("ADMIN_RESULT", "OK", sid, true)); }

                case "ADMIN_CANCEL_AUCTION"-> { adminService.cancelAuction(req.getTargetId());
                    send(new AdminResponse("ADMIN_RESULT", "OK", sid, true)); }

                case "ADMIN_DELETE_AUCTION"-> { adminService.deleteAuctionById(req.getTargetId());
                    send(new AdminResponse("ADMIN_RESULT", "OK", sid, true)); }

                case "ADMIN_DELETE_ITEM"   -> { adminService.deleteItemById(req.getTargetId());
                    send(new AdminResponse("ADMIN_RESULT", "OK", sid, true)); }

                default -> send(new AdminResponse("ADMIN_RESULT", "Unknown action", sid, false));
            }
        } catch (AuthenticationException e) {
            send(new AdminResponse("ADMIN_RESULT", e.getMessage(), null, false));
        }
    }
}