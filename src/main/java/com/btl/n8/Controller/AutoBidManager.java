package com.btl.n8.Controller;

import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Enums.AuctionStatus;
import com.btl.n8.DTO.BidRequest;
import com.btl.n8.DTO.BidResponse;
import com.btl.n8.Network.ClientSocket;
import com.btl.n8.Network.ServerResponseListener;
import com.btl.n8.Util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javafx.application.Platform;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Singleton quản lý AutoBid, sống độc lập với UI.
 * Bid Sniping đã bị loại bỏ — chỉ còn AutoBid thông thường:
 * tự động outbid khi bị người khác vượt giá, miễn là nextBid <= maxPrice.
 */
public class AutoBidManager implements ServerResponseListener {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static volatile AutoBidManager instance;

    public static AutoBidManager getInstance() {
        if (instance == null) {
            synchronized (AutoBidManager.class) {
                if (instance == null) instance = new AutoBidManager();
            }
        }
        return instance;
    }

    private AutoBidManager() {}

    // ── Session data per auction ──────────────────────────────────────────────
    public static class AutoBidSession {
        public final int        auctionId;
        public final int        bidderId;
        public final BigDecimal maxPrice;
        public final BigDecimal step;
        public       Auction    auction;
        public final AtomicBoolean active = new AtomicBoolean(true);

        public AutoBidSession(int auctionId, int bidderId,
                              BigDecimal maxPrice, BigDecimal step,
                              Auction auction) {
            this.auctionId = auctionId;
            this.bidderId  = bidderId;
            this.maxPrice  = maxPrice;
            this.step      = step;
            this.auction   = auction;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private final Map<Integer, AutoBidSession>  sessions    = new ConcurrentHashMap<>();
    private final Map<Integer, Consumer<String>> uiCallbacks = new ConcurrentHashMap<>();
    private final AtomicBoolean listenerRegistered = new AtomicBoolean(false);

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean isActive(int auctionId) {
        AutoBidSession s = sessions.get(auctionId);
        return s != null && s.active.get();
    }

    public AutoBidSession getSession(int auctionId) {
        return sessions.get(auctionId);
    }

    /** Kích hoạt AutoBid. Gọi từ AutoBidController khi nhấn Activate. */
    public void activate(int auctionId, int bidderId,
                         BigDecimal maxPrice, BigDecimal step,
                         Auction auction) {
        cancelSilent(auctionId);

        AutoBidSession session = new AutoBidSession(
                auctionId, bidderId, maxPrice, step, auction);
        sessions.put(auctionId, session);

        if (listenerRegistered.compareAndSet(false, true)) {
            ClientSocket.getInstance().addListener(this);
        }

        // Check ngay xem có đang bị outbid không
        checkAndBid(session);
    }

    /** Huỷ AutoBid và thông báo UI. Chỉ gọi khi người dùng nhấn Cancel. */
    public void cancel(int auctionId) {
        cancelSilent(auctionId);
        notifyUI(auctionId, "CANCELLED");
    }

    /** Huỷ tất cả — gọi khi logout. */
    public void cancelAll() {
        for (int id : sessions.keySet()) {
            cancelSilent(id);
            notifyUI(id, "CANCELLED");
        }
        if (listenerRegistered.compareAndSet(true, false)) {
            ClientSocket.getInstance().removeListener(this);
        }
        uiCallbacks.clear();
    }

    /** Cập nhật auction reference khi có giá/endTime mới. */
    public void updateAuction(int auctionId, Auction auction) {
        AutoBidSession s = sessions.get(auctionId);
        if (s != null) s.auction = auction;
    }

    public void setUICallback(int auctionId, Consumer<String> callback) {
        if (callback == null) uiCallbacks.remove(auctionId);
        else                  uiCallbacks.put(auctionId, callback);
    }

    // ── ServerResponseListener ────────────────────────────────────────────────

    @Override
    public void onRespone(JsonObject response) {
        if (!response.has("action")) return;
        if (!"BID_UPDATE".equals(response.get("action").getAsString())) return;

        BidResponse res = gson.fromJson(response, BidResponse.class);
        if (!res.isSuccess()) return;

        int auctionId = res.getAuctionId();
        AutoBidSession session = sessions.get(auctionId);
        if (session == null || !session.active.get()) return;

        if (session.auction != null) {
            session.auction.setCurrentPrice(res.getCurrentPrice());
            // Cập nhật endTime nếu server gia hạn (Anti-sniping)
            if (res.getNewEndTime() != null) {
                session.auction.setEndTime(res.getNewEndTime());
            }
        }

        // Chỉ react khi người khác bid
        if (res.getBidderId() != session.bidderId) {
            Platform.runLater(() -> checkAndBid(session));
        }
    }

    // ── AutoBid logic ─────────────────────────────────────────────────────────

    private void checkAndBid(AutoBidSession session) {
        if (!session.active.get() || session.auction == null) return;

        if (session.auction.getStatus() != AuctionStatus.OPEN) {
            cancelSilent(session.auctionId);
            notifyUI(session.auctionId, "STOPPED:Auction closed – AutoBid stopped.");
            return;
        }

        BigDecimal nextBid = session.auction.getCurrentPrice().add(session.step);

        if (nextBid.compareTo(session.maxPrice) > 0) {
            cancelSilent(session.auctionId);
            notifyUI(session.auctionId, "STOPPED:Max price reached – AutoBid stopped.");
            return;
        }

        new Thread(() -> {
            BidRequest req = new BidRequest(session.auctionId, session.bidderId, nextBid);
            req.setSessionId(SessionManager.getInstance().getSessionId());
            ClientSocket.getInstance().sendMessage(req);
            notifyUI(session.auctionId, "BID_PLACED:" + nextBid.toPlainString());
        }).start();
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void cancelSilent(int auctionId) {
        AutoBidSession s = sessions.remove(auctionId);
        if (s != null) s.active.set(false);
        if (sessions.isEmpty() && listenerRegistered.compareAndSet(true, false)) {
            ClientSocket.getInstance().removeListener(this);
        }
    }

    private void notifyUI(int auctionId, String event) {
        Consumer<String> cb = uiCallbacks.get(auctionId);
        if (cb != null) Platform.runLater(() -> cb.accept(event));
    }
}