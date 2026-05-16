package com.btl.n8.Controller;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Bid;
import com.btl.n8.Model.Entity.Item;
import com.btl.n8.Model.Enums.AuctionStatus;
import com.btl.n8.Service.AuctionService;
import com.btl.n8.Service.BidService;
import com.btl.n8.DTO.BidRequest;
import com.btl.n8.DTO.BidResponse;
import com.btl.n8.Network.ClientSocket;
import com.btl.n8.Network.ServerResponseListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.btl.n8.Util.LocalDateTimeAdapter;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BidDetailController implements ServerResponseListener {

    @FXML private ImageView itemImage;
    @FXML private Label itemName;
    @FXML private Label itemId;
    @FXML private Label itemType;
    @FXML private Label itemStatus;
    @FXML private Label currentPrice;
    @FXML private Label timerLabel;
    @FXML private Label bidMsg;
    @FXML private TextField bidInput;
    @FXML private LineChart<String, Number> bidChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Button autoBidButton;

    private int auctionId;
    private int bidderId;
    private Auction auction;
    private Item item;
    private Timer countdownTimer;
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private AuctionService auctionService;
    private BidService bidService;

    // ── AutoBid popup state ───────────────────────────────────────────────────
    // FIX: Giữ thêm autoBidRoot để có thể mở lại popup với cùng controller
    //      khi AutoBid đang chạy ngầm (popup đã đóng nhưng chưa cancel).
    private Stage             autoBidStage;
    private AutoBidController autoBidCtrl;
    private Parent            autoBidRoot;   // scene root từ lần load FXML đầu tiên

    // ── Style constants ───────────────────────────────────────────────────────
    private static final String STYLE_AUTOBID_DEFAULT =
            "-fx-background-color: #0f2035; -fx-text-fill: #ffd700; " +
                    "-fx-border-color: #ffd700; -fx-border-radius: 6; " +
                    "-fx-background-radius: 6; -fx-cursor: hand; -fx-border-width: 1.5;";

    private static final String STYLE_AUTOBID_ACTIVE =
            "-fx-background-color: #3a1a1a; -fx-text-fill: #ff6b6b; " +
                    "-fx-border-color: #ff6b6b; -fx-border-radius: 6; " +
                    "-fx-background-radius: 6; -fx-cursor: hand; -fx-border-width: 1.5;";

    /**
     * Được gọi từ bidController (trên FX thread) ngay sau loader.load().
     */
    public void initData(int auctionId, Item item) {
        this.auctionId = auctionId;
        this.bidderId  = SessionManager.getInstance().getCurrentUser().getId();
        this.item      = item;

        // ── Bước 1: Set UI từ Item ngay (không cần DB, không block) ──────────
        itemName.setText(item.getName());
        itemId.setText("#" + item.getId());
        itemType.setText(item.getType().name());
        timerLabel.setText("Loading...");
        currentPrice.setText("Loading...");
        itemStatus.setText("-");
        bidInput.setDisable(true);

        if (item.getImage() != null && item.getImage().length > 0) {
            try {
                Image img = new Image(new java.io.ByteArrayInputStream(item.getImage()));
                itemImage.setImage(img);
            } catch (Exception e) {
                System.out.println("Không load được ảnh item");
            }
        }

        // Đăng ký lắng nghe broadcast ngay
        ClientSocket.getInstance().addListener(this);

        // ── Bước 2: Load DB trong background thread ───────────────────────────
        new Thread(() -> {
            try {
                Connection conn = DataConnection.getConnection();
                if (conn == null) throw new Exception("Database connection failed");

                auctionService = new AuctionService(new AuctionDAOImpl(conn));
                bidService     = new BidService(new BidDAOImpl(conn));
                Auction loaded = auctionService.getAuctionById(auctionId);

                Platform.runLater(() -> {
                    auction = loaded;
                    updateAuctionStatus();
                    loadChart();

                    if (auction != null) {
                        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
                            bidInput.setDisable(true);
                            timerLabel.setText("Ended");
                            reloadAuction();
                        } else {
                            startCountdown(auction.getEndTime());
                        }
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showMsg("Failed to load data. Please try again.", false);
                    timerLabel.setText("Error");
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void updateAuctionStatus() {
        if (auction == null) {
            itemStatus.setText("-");
            currentPrice.setText("-");
            bidInput.setDisable(true);
            return;
        }

        currentPrice.setText(fmt(auction.getCurrentPrice()));
        itemStatus.setText(auction.getStatus().name());

        switch (auction.getStatus()) {
            case OPEN -> {
                itemStatus.setStyle(
                        "-fx-background-color: #00ff88; -fx-text-fill: #0a1628; " +
                                "-fx-padding: 2 8 2 8; -fx-background-radius: 99;");
                bidInput.setDisable(false);
            }
            case CLOSED -> {
                itemStatus.setStyle(
                        "-fx-background-color: #888888; -fx-text-fill: white; " +
                                "-fx-padding: 2 8 2 8; -fx-background-radius: 99;");
                bidInput.setDisable(true);
            }
            case CANCELLED -> {
                itemStatus.setStyle(
                        "-fx-background-color: #ff6b6b; -fx-text-fill: white; " +
                                "-fx-padding: 2 8 2 8; -fx-background-radius: 99;");
                bidInput.setDisable(true);
            }
        }
    }

    private void reloadAuction() {
        new Thread(() -> {
            Auction updated = auctionService.getAuctionById(auctionId);
            Platform.runLater(() -> {
                auction = updated;
                updateAuctionStatus();
            });
        }).start();
    }

    private void loadChart() {
        new Thread(() -> {
            List<Bid> bids = bidService.getBidsByAuction(auctionId);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Price");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

            for (int i = bids.size() - 1; i >= 0; i--) {
                Bid bid = bids.get(i);
                series.getData().add(new XYChart.Data<>(
                        bid.getBidTime().format(fmt),
                        bid.getAmount()
                ));
            }

            Platform.runLater(() -> {
                bidChart.getData().clear();
                bidChart.getData().add(series);
            });
        }).start();
    }

    @FXML
    public void handlePlaceBid() {
        bidMsg.setText("");
        String text = bidInput.getText().trim();

        if (text.isEmpty()) { showMsg("Please enter a bid amount", false); return; }

        BigDecimal amount;
        try {
            amount = new BigDecimal(text);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showMsg("Amount must be positive", false);
                return;
            }
        } catch (NumberFormatException e) {
            showMsg("Invalid amount format", false);
            return;
        }

        new Thread(() -> {
            Auction latest = auctionService.getAuctionById(auctionId);
            Platform.runLater(() -> {
                auction = latest;

                if (auction == null) {
                    showMsg("Auction not available", false);
                    return;
                }
                if (auction.getStatus() != AuctionStatus.OPEN) {
                    updateAuctionStatus();
                    showMsg("Auction is not open!", false);
                    return;
                }
                if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
                    showMsg("Bid must be higher than " + fmt(auction.getCurrentPrice()), false);
                    return;
                }

                currentPrice.setText(fmt(amount));
                if (auction != null) auction.setCurrentPrice(amount);

                new Thread(() -> {
                    BidRequest req = new BidRequest(auctionId, bidderId, amount);
                    req.setSessionId(SessionManager.getInstance().getSessionId());
                    ClientSocket.getInstance().sendMessage(req);
                }).start();

                bidInput.clear();
                showMsg("Bid sent...", true);
            });
        }).start();
    }

    @Override
    public void onRespone(JsonObject response) {
        if (!response.has("action")) return;
        String action = response.get("action").getAsString();
        if (!"BID_UPDATE".equals(action)) return;

        BidResponse res = gson.fromJson(response, BidResponse.class);
        if (res.getAuctionId() != this.auctionId) return;

        Platform.runLater(() -> {
            if (res.isSuccess()) {
                currentPrice.setText(fmt(res.getCurrentPrice()));
                if (auction != null) {
                    auction.setCurrentPrice(res.getCurrentPrice());
                }
                if (res.getBidderId() != bidderId) {
                    showMsg("⚡ Someone placed: " + fmt(res.getCurrentPrice()), true);
                } else {
                    showMsg("✓ Your bid was placed!", true);
                }
                if (bidService != null) {
                    loadChart();
                }
            } else {
                if (auction != null) {
                    currentPrice.setText(fmt(res.getCurrentPrice()));
                    auction.setCurrentPrice(res.getCurrentPrice());
                }
                showMsg(res.getMessage() != null ? res.getMessage() : "Bid failed", false);
            }
        });
    }

    private void startCountdown(LocalDateTime endTime) {
        countdownTimer = new Timer(true);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long diff = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
                if (diff <= 0) {
                    Platform.runLater(() -> {
                        timerLabel.setText("Ended");
                        bidInput.setDisable(true);
                    });

                    new Thread(() -> {
                        Auction updated = auctionService.getAuctionById(auctionId);
                        Platform.runLater(() -> {
                            auction = updated;
                            updateAuctionStatus();
                            showMsg("Auction has ended!", false);
                        });
                    }).start();

                    countdownTimer.cancel();
                    return;
                }
                long h = diff / 3600;
                long m = (diff % 3600) / 60;
                long s = diff % 60;
                Platform.runLater(() ->
                        timerLabel.setText(String.format("%02d:%02d:%02d", h, m, s)));
            }
        }, 0, 1000);
    }

    public void cleanup() {
        if (countdownTimer != null) countdownTimer.cancel();
        ClientSocket.getInstance().removeListener(this);
        // Dọn autoBid nếu còn sót lại
        if (autoBidCtrl != null) {
            autoBidCtrl.cleanup();
            autoBidCtrl  = null;
            autoBidRoot  = null;
            autoBidStage = null;
        }
    }

    /**
     * FIX AUTOBID POPUP – toàn bộ logic mở/tái hiện popup.
     *
     * Có 3 trường hợp khi người dùng nhấn nút AutoBid:
     *
     *  A) Popup đang mở → focus lại (giữ nguyên như cũ).
     *
     *  B) AutoBid đang chạy ngầm (popup đã đóng, ctrl != null, ctrl.isActive())
     *     → Mở lại popup với scene cũ (autoBidRoot), không load lại FXML,
     *       không tạo controller mới → listener vẫn sống, timer không bị reset.
     *
     *  C) Không có AutoBid nào → load FXML mới, setup bình thường.
     *
     * Khi popup đóng (setOnHidden):
     *  - Nếu AutoBid đang active: GIỮ ctrl & root, chuyển nút thành "⚡ ON — Cancel".
     *  - Nếu AutoBid không active: cleanup ctrl, nút về trạng thái mặc định.
     */
    @FXML
    public void handleOpenAutoBid() {

        // ── Trường hợp A: popup đang mở → focus lại ──────────────────────────
        if (autoBidStage != null && autoBidStage.isShowing()) {
            autoBidStage.requestFocus();
            return;
        }

        // ── Trường hợp B: AutoBid đang chạy ngầm → mở lại stage với root cũ ─
        if (autoBidCtrl != null && autoBidCtrl.isActive() && autoBidRoot != null) {
            autoBidStage = new Stage();
            autoBidStage.initStyle(StageStyle.UNDECORATED);
            autoBidStage.setTitle("AutoBid");
            autoBidStage.setScene(new Scene(autoBidRoot));
            autoBidStage.initModality(Modality.NONE);
            autoBidStage.setResizable(false);

            // Khôi phục UI của popup về đúng trạng thái active
            autoBidCtrl.restoreActiveUI();

            attachOnHidden(autoBidStage);
            autoBidStage.show();
            return;
        }

        // ── Trường hợp C: Tạo mới hoàn toàn ─────────────────────────────────
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AutoBid.fxml"));
            autoBidRoot = loader.load();

            autoBidCtrl = loader.getController();
            autoBidCtrl.initData(auctionId, item, auction);

            // Callback: khi AutoBid active/inactive → cập nhật nút bên ngoài
            autoBidCtrl.setOnActiveChanged(active -> Platform.runLater(() -> {
                if (active) {
                    setAutoBidButtonActive();
                } else {
                    setAutoBidButtonDefault();
                }
            }));

            autoBidStage = new Stage();
            autoBidStage.initStyle(StageStyle.UNDECORATED);
            autoBidStage.setTitle("AutoBid");
            autoBidStage.setScene(new Scene(autoBidRoot));
            autoBidStage.initModality(Modality.NONE);
            autoBidStage.setResizable(false);

            attachOnHidden(autoBidStage);
            autoBidStage.show();

        } catch (Exception ex) {
            showMsg("Failed to open AutoBid: " + ex.getMessage(), false);
            ex.printStackTrace();
        }
    }

    /**
     * FIX: Gắn setOnHidden với logic phân biệt "đóng khi active" vs "đóng khi không active".
     *
     * - Đóng khi ACTIVE  → KHÔNG cleanup, KHÔNG reset nút.
     *   autoBidCtrl và autoBidRoot được giữ lại để mở lại popup sau.
     *   Nút vẫn là "⚡ ON — Cancel" và khi nhấn sẽ cancel (không mở popup mới).
     *
     * - Đóng khi KHÔNG ACTIVE → cleanup ctrl, reset nút về mặc định.
     */
    private void attachOnHidden(Stage stage) {
        stage.setOnHidden(e -> {
            boolean stillActive = autoBidCtrl != null && autoBidCtrl.isActive();
            autoBidStage = null; // stage đã đóng, giải phóng reference

            if (stillActive) {
                // AutoBid vẫn chạy ngầm – giữ ctrl & root, nút vẫn "⚡ ON — Cancel"
                // (nút đã được set ở callback onActiveChanged khi Activate)
                // Không làm gì thêm.
            } else {
                // Đóng popup khi không active → dọn sạch
                if (autoBidCtrl != null) {
                    autoBidCtrl.cleanup();
                    autoBidCtrl = null;
                }
                autoBidRoot = null;
                setAutoBidButtonDefault();
            }
        });
    }

    // ── Nút AutoBid helpers ───────────────────────────────────────────────────

    private void setAutoBidButtonActive() {
        autoBidButton.setText("⚡ ON — Cancel");
        autoBidButton.setStyle(STYLE_AUTOBID_ACTIVE);
        autoBidButton.setOnAction(ev -> {
            // Nhấn nút khi active → cancel AutoBid (không mở popup)
            if (autoBidCtrl != null) {
                autoBidCtrl.cancelFromOutside();
            }
            // cancelFromOutside() → deactivate() → onActiveChanged.accept(false)
            // → setAutoBidButtonDefault() sẽ được gọi tự động qua callback
        });
    }

    private void setAutoBidButtonDefault() {
        autoBidButton.setText("⚡ AutoBid");
        autoBidButton.setStyle(STYLE_AUTOBID_DEFAULT);
        autoBidButton.setOnAction(ev -> handleOpenAutoBid());
        // Sau khi cancel, dọn ctrl & root nếu popup không còn mở
        if (autoBidStage == null) {
            autoBidCtrl = null;
            autoBidRoot = null;
        }
    }

    @FXML
    public void handleClose() {
        cleanup();
        Stage stage = (Stage) bidInput.getScene().getWindow();
        stage.close();
    }

    private void showMsg(String msg, boolean success) {
        bidMsg.setStyle(success
                ? "-fx-text-fill: #00ff88; -fx-font-size: 12px;"
                : "-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
        bidMsg.setText(msg);
    }

    private String fmt(BigDecimal n) {
        return String.format("%,.0f ₫", n);
    }
}