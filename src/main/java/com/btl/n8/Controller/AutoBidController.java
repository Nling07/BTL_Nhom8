package com.btl.n8.Controller;

import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Item;
import com.btl.n8.Model.Enums.AuctionStatus;
import com.btl.n8.DTO.BidRequest;
import com.btl.n8.DTO.BidResponse;
import com.btl.n8.Network.ClientSocket;
import com.btl.n8.Network.ServerResponseListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.btl.n8.Util.LocalDateTimeAdapter;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.function.Consumer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller cho popup AutoBid.fxml.
 *
 * Tính năng:
 *  1. AutoBid thông thường – tự động outbid khi có người bid cao hơn,
 *     miễn là giá mới <= maxPrice.
 *  2. Bid Sniping – chỉ kích hoạt bid trong N giây cuối của phiên
 *     (người khác không kịp phản ứng).
 *
 * Lifecycle:
 *  bidDetailController mở popup → gọi initData(auctionId, item, auction)
 *  → người dùng điền form và nhấn "Activate"
 *  → controller đăng ký lắng nghe BID_UPDATE broadcast
 *  → khi bị outbid → tự động gửi BidRequest qua socket
 *
 * FIX: Thêm isActive() để bidDetailController biết trạng thái khi popup đóng.
 *      Thêm restoreActiveUI() để khi popup mở lại có thể hiện đúng trạng thái.
 */
public class AutoBidController implements ServerResponseListener {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Label    lblItemName;
    @FXML private Label    lblCurrentPrice;
    @FXML private Label    statusBadge;
    @FXML private Label    msgLabel;

    @FXML private TextField maxPriceField;
    @FXML private TextField stepField;
    @FXML private CheckBox  snipeModeCheck;
    @FXML private HBox snipeRow;
    @FXML private Spinner<Integer> snipeSecondsSpinner;

    @FXML private Button activateBtn;
    @FXML private Button cancelAutoBidBtn;
    @FXML private Button closeBtn;

    // ── State ────────────────────────────────────────────────────────────────
    private int          auctionId;
    private int          bidderId;
    private Auction      auction;          // live reference, updated on BID_UPDATE
    private Item         item;

    private BigDecimal   maxPrice;
    private BigDecimal   step;
    private boolean      snipeMode;
    private int          snipeSeconds;

    private final AtomicBoolean autoBidActive = new AtomicBoolean(false);
    private Consumer<Boolean> onActiveChanged; // callback -> bidDetailController
    private Timer        snipeTimer;           // fires near endTime

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    // ── Public API ───────────────────────────────────────────────────────────

    /** bidDetailController đăng ký để nhận sự kiện active/inactive */
    public void setOnActiveChanged(Consumer<Boolean> cb) {
        this.onActiveChanged = cb;
    }

    /**
     * FIX: Cho phép bidDetailController kiểm tra trạng thái trước khi đóng popup.
     * Dùng để phân biệt "đóng popup khi đang chạy" vs "đóng popup khi đã dừng".
     */
    public boolean isActive() {
        return autoBidActive.get();
    }

    /** Huỷ AutoBid từ nút trên bidDetails (không cần mở popup) */
    public void cancelFromOutside() {
        deactivate();
        showMsg("AutoBid cancelled.", false);
    }

    /**
     * FIX: Gọi khi popup được mở lại sau khi đã đóng nhưng AutoBid vẫn đang chạy ngầm.
     * Khôi phục UI về đúng trạng thái active mà không reset form hay listener.
     */
    public void restoreActiveUI() {
        if (autoBidActive.get()) {
            // Điền lại giá trị vào form (readonly)
            if (maxPrice != null) maxPriceField.setText(maxPrice.toPlainString());
            if (step != null)     stepField.setText(step.toPlainString());
            snipeModeCheck.setSelected(snipeMode);
            if (snipeMode) {
                snipeRow.setVisible(true);
                snipeRow.setManaged(true);
                snipeSecondsSpinner.getValueFactory().setValue(snipeSeconds);
            }
            // Cập nhật giá hiện tại
            if (auction != null) {
                lblCurrentPrice.setText(fmt(auction.getCurrentPrice()));
            }
            setActiveUI(true);
            showMsg("AutoBid đang chạy ngầm...", true);
        }
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    /**
     * Gọi từ bidDetailController ngay sau loader.load().
     */
    public void initData(int auctionId, Item item, Auction auction) {
        this.auctionId = auctionId;
        this.bidderId  = SessionManager.getInstance().getCurrentUser().getId();
        this.item      = item;
        this.auction   = auction;

        lblItemName.setText(item.getName());
        if (auction != null) {
            lblCurrentPrice.setText(fmt(auction.getCurrentPrice()));
        }
    }

    // ── FXML handlers ────────────────────────────────────────────────────────

    @FXML
    public void onSnipeToggle() {
        boolean on = snipeModeCheck.isSelected();
        snipeRow.setVisible(on);
        snipeRow.setManaged(on);
    }

    @FXML
    public void handleActivate() {
        msgLabel.setText("");

        // --- Validate max price ---
        String maxStr = maxPriceField.getText().trim();
        if (maxStr.isEmpty()) { showMsg("Please enter Max Price", false); return; }
        try {
            maxPrice = new BigDecimal(maxStr);
            if (maxPrice.compareTo(BigDecimal.ZERO) <= 0) {
                showMsg("Max Price must be positive", false); return;
            }
        } catch (NumberFormatException e) {
            showMsg("Invalid Max Price format", false); return;
        }

        // --- Validate step ---
        String stepStr = stepField.getText().trim();
        if (stepStr.isEmpty()) { showMsg("Please enter Bid Step", false); return; }
        try {
            step = new BigDecimal(stepStr);
            if (step.compareTo(BigDecimal.ZERO) <= 0) {
                showMsg("Bid Step must be positive", false); return;
            }
        } catch (NumberFormatException e) {
            showMsg("Invalid Bid Step format", false); return;
        }

        // --- Validate against current price ---
        if (auction != null && maxPrice.compareTo(auction.getCurrentPrice()) <= 0) {
            showMsg("Max Price must be above current price:\n" + fmt(auction.getCurrentPrice()), false);
            return;
        }

        snipeMode    = snipeModeCheck.isSelected();
        snipeSeconds = snipeModeCheck.isSelected()
                ? snipeSecondsSpinner.getValue()
                : 0;

        // Activate
        autoBidActive.set(true);
        ClientSocket.getInstance().addListener(this);
        if (onActiveChanged != null) onActiveChanged.accept(true);

        // UI feedback
        setActiveUI(true);
        showMsg("AutoBid active! Watching for outbids...", true);

        // Schedule snipe timer if needed
        if (snipeMode && auction != null) {
            scheduleSnipe(auction.getEndTime(), snipeSeconds);
        }

        // If not snipe-only: check immediately whether we're already outbid
        if (!snipeMode) {
            checkAndBidIfOutbid();
        }
    }

    @FXML
    public void handleCancelAutoBid() {
        deactivate();
        showMsg("AutoBid cancelled.", false);
    }

    /**
     * FIX: Nút Close chỉ đóng popup, KHÔNG deactivate AutoBid nếu đang active.
     * bidDetailController sẽ xử lý trạng thái nút ở setOnHidden.
     */
    @FXML
    public void handleClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
        // Không gọi cleanup() ở đây — để AutoBid tiếp tục chạy ngầm nếu đang active.
        // bidDetailController.setOnHidden() sẽ quyết định có cleanup hay không.
    }

    // ── ServerResponseListener ───────────────────────────────────────────────

    @Override
    public void onRespone(JsonObject response) {
        if (!response.has("action")) return;
        String action = response.get("action").getAsString();

        if ("BID_UPDATE".equals(action)) {
            BidResponse res = gson.fromJson(response, BidResponse.class);
            if (res.getAuctionId() != auctionId) return;
            if (!res.isSuccess()) return;

            Platform.runLater(() -> {
                // Update local price reference
                if (auction != null) {
                    auction.setCurrentPrice(res.getCurrentPrice());
                }
                lblCurrentPrice.setText(fmt(res.getCurrentPrice()));

                // Someone else bid – react only if autoBid is active and not in snipe-only mode
                if (autoBidActive.get() && !snipeMode) {
                    // Only react if we were outbid (not our own bid)
                    if (res.getBidderId() != bidderId) {
                        checkAndBidIfOutbid();
                    }
                }
            });
        }
    }

    // ── AutoBid logic ────────────────────────────────────────────────────────

    /**
     * Kiểm tra xem ta có đang bị outbid không và nếu vẫn trong budget thì tự bid.
     */
    private void checkAndBidIfOutbid() {
        if (!autoBidActive.get() || auction == null) return;
        if (auction.getStatus() != AuctionStatus.OPEN) {
            deactivate();
            showMsg("Auction closed – AutoBid stopped.", false);
            return;
        }

        BigDecimal current = auction.getCurrentPrice();
        BigDecimal nextBid = current.add(step);

        if (nextBid.compareTo(maxPrice) > 0) {
            Platform.runLater(() -> {
                showMsg("Max price reached – AutoBid stopped.", false);
                deactivate();
            });
            return;
        }

        // Send bid via socket
        new Thread(() -> {
            BidRequest req = new BidRequest(auctionId, bidderId, nextBid);
            req.setSessionId(SessionManager.getInstance().getSessionId());
            ClientSocket.getInstance().sendMessage(req);
            Platform.runLater(() ->
                    showMsg("🤖 AutoBid placed: " + fmt(nextBid), true));
        }).start();
    }

    /**
     * Bid Sniping: schedule một timer để bắn bid ngay trước khi endTime.
     */
    private void scheduleSnipe(LocalDateTime endTime, int secondsBefore) {
        if (endTime == null) return;

        long delayMs = java.time.Duration.between(
                LocalDateTime.now(),
                endTime.minusSeconds(secondsBefore)
        ).toMillis();

        if (delayMs <= 0) {
            // Đã qua thời điểm snipe – thực hiện ngay
            triggerSnipeBid();
            return;
        }

        snipeTimer = new Timer(true);
        snipeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!autoBidActive.get()) return;
                triggerSnipeBid();
            }
        }, delayMs);

        Platform.runLater(() ->
                showMsg("🎯 Snipe scheduled – fires " + secondsBefore + "s before end!", true));
    }

    /**
     * Thực hiện bid khi timer snipe kích hoạt.
     */
    private void triggerSnipeBid() {
        if (!autoBidActive.get()) return;

        Platform.runLater(() -> showMsg("🎯 Sniping now!", true));

        new Thread(() -> {
            if (auction == null) return;

            BigDecimal current = auction.getCurrentPrice();
            BigDecimal snipeBid = current.add(step);

            if (snipeBid.compareTo(maxPrice) > 0) {
                Platform.runLater(() -> {
                    showMsg("Max price reached – can't snipe.", false);
                    deactivate();
                });
                return;
            }

            BidRequest req = new BidRequest(auctionId, bidderId, snipeBid);
            req.setSessionId(SessionManager.getInstance().getSessionId());
            ClientSocket.getInstance().sendMessage(req);

            Platform.runLater(() ->
                    showMsg("🎯 Snipe bid sent: " + fmt(snipeBid), true));
        }).start();
    }

    // ── UI helpers ───────────────────────────────────────────────────────────

    private void setActiveUI(boolean active) {
        if (active) {
            statusBadge.setText("ACTIVE");
            statusBadge.setStyle(
                    "-fx-background-color: #00ff88; -fx-text-fill: #0a1628; " +
                            "-fx-padding: 3 10 3 10; -fx-background-radius: 99;");
            activateBtn.setVisible(false);
            activateBtn.setManaged(false);
            cancelAutoBidBtn.setVisible(true);
            cancelAutoBidBtn.setManaged(true);
            maxPriceField.setDisable(true);
            stepField.setDisable(true);
            snipeModeCheck.setDisable(true);
            snipeSecondsSpinner.setDisable(true);
        } else {
            statusBadge.setText("OFF");
            statusBadge.setStyle(
                    "-fx-background-color: #333a4a; -fx-text-fill: #888; " +
                            "-fx-padding: 3 10 3 10; -fx-background-radius: 99;");
            activateBtn.setVisible(true);
            activateBtn.setManaged(true);
            cancelAutoBidBtn.setVisible(false);
            cancelAutoBidBtn.setManaged(false);
            maxPriceField.setDisable(false);
            stepField.setDisable(false);
            snipeModeCheck.setDisable(false);
            snipeSecondsSpinner.setDisable(false);
        }
    }

    private void showMsg(String msg, boolean success) {
        Platform.runLater(() -> {
            msgLabel.setStyle(success
                    ? "-fx-text-fill: #00ff88;"
                    : "-fx-text-fill: #ff6b6b;");
            msgLabel.setText(msg);
        });
    }

    private String fmt(BigDecimal n) {
        return String.format("%,.0f ₫", n);
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    private void deactivate() {
        autoBidActive.set(false);
        if (snipeTimer != null) { snipeTimer.cancel(); snipeTimer = null; }
        ClientSocket.getInstance().removeListener(this);
        Platform.runLater(() -> {
            setActiveUI(false);
            if (onActiveChanged != null) onActiveChanged.accept(false);
        });
    }

    /**
     * FIX: cleanup() chỉ được gọi từ bidDetailController khi thực sự muốn dừng hẳn
     * (ví dụ: bidDetailController bị đóng, hoặc popup đóng khi AutoBid không active).
     * Không còn được gọi tự động từ handleClose().
     */
    public void cleanup() {
        deactivate();
    }
}