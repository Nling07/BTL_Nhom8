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

    // Phải khớp với RequestHandler.SNIPE_EXTENSION_SECONDS để hiện thông báo đúng
    private static final int SNIPE_EXTENSION_SECONDS = 30;

    private AuctionService auctionService;
    private BidService bidService;

    // ── AutoBid popup state ───────────────────────────────────────────────────
    // Chỉ giữ Stage và Controller của popup AutoBid.
    // State thực sự (listener, timer, bid logic) nằm trong AutoBidManager.
    private Stage             autoBidStage;
    private AutoBidController autoBidCtrl;

    private static final String STYLE_DEFAULT =
            "-fx-background-color: #0f2035; -fx-text-fill: #ffd700; " +
                    "-fx-border-color: #ffd700; -fx-border-radius: 6; " +
                    "-fx-background-radius: 6; -fx-cursor: hand; -fx-border-width: 1.5;";

    private static final String STYLE_ACTIVE =
            "-fx-background-color: #3a1a1a; -fx-text-fill: #ff6b6b; " +
                    "-fx-border-color: #ff6b6b; -fx-border-radius: 6; " +
                    "-fx-background-radius: 6; -fx-cursor: hand; -fx-border-width: 1.5;";

    // ── Init ──────────────────────────────────────────────────────────────────

    public void initData(int auctionId, Item item) {
        this.auctionId = auctionId;
        this.bidderId  = SessionManager.getInstance().getCurrentUser().getId();
        this.item      = item;

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

        ClientSocket.getInstance().addListener(this);

        // Đăng ký callback từ AutoBidManager để nút phản ánh đúng trạng thái
        // (quan trọng: khi mở lại bidDetails, AutoBid có thể đang chạy từ lần trước)
        AutoBidManager.getInstance().setUICallback(auctionId, this::handleAutoBidManagerEvent);

        // Cập nhật nút ngay nếu AutoBid đang active cho auction này
        if (AutoBidManager.getInstance().isActive(auctionId)) {
            setAutoBidButtonActive();
        }

        new Thread(() -> {
            try {
                Connection conn = DataConnection.getConnection();
                if (conn == null) throw new Exception("Database connection failed");

                auctionService = new AuctionService(new AuctionDAOImpl(conn));
                bidService     = new BidService(new BidDAOImpl(conn));
                Auction loaded = auctionService.getAuctionById(auctionId);

                Platform.runLater(() -> {
                    auction = loaded;
                    // Đồng bộ auction reference mới vào Manager
                    AutoBidManager.getInstance().updateAuction(auctionId, auction);
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

    // ── AutoBidManager event handler ──────────────────────────────────────────

    /**
     * Nhận event từ AutoBidManager để cập nhật nút AutoBid trên bidDetails.
     * Chạy trên FX thread (Manager đã wrap bằng Platform.runLater).
     */
    private void handleAutoBidManagerEvent(String event) {
        if (event == null) return;
        String type = event.split(":", 2)[0];
        switch (type) {
            case "CANCELLED", "STOPPED" -> setAutoBidButtonDefault();
            // BID_PLACED, SNIPE_* → popup tự hiển thị, không cần xử lý ở đây
        }
    }

    // ── Auction status ────────────────────────────────────────────────────────

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
                itemStatus.setStyle("-fx-background-color: #00ff88; -fx-text-fill: #0a1628; " +
                        "-fx-padding: 2 8 2 8; -fx-background-radius: 99;");
                bidInput.setDisable(false);
            }
            case CLOSED -> {
                itemStatus.setStyle("-fx-background-color: #888888; -fx-text-fill: white; " +
                        "-fx-padding: 2 8 2 8; -fx-background-radius: 99;");
                bidInput.setDisable(true);
            }
            case CANCELLED -> {
                itemStatus.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; " +
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
                AutoBidManager.getInstance().updateAuction(auctionId, auction);
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
                series.getData().add(new XYChart.Data<>(bid.getBidTime().format(fmt), bid.getAmount()));
            }
            Platform.runLater(() -> {
                bidChart.getData().clear();
                bidChart.getData().add(series);
            });
        }).start();
    }

    // ── Bid ───────────────────────────────────────────────────────────────────

    @FXML
    public void handlePlaceBid() {
        bidMsg.setText("");
        String text = bidInput.getText().trim();
        if (text.isEmpty()) { showMsg("Please enter a bid amount", false); return; }

        BigDecimal amount;
        try {
            amount = new BigDecimal(text);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showMsg("Amount must be positive", false); return;
            }
        } catch (NumberFormatException e) {
            showMsg("Invalid amount format", false); return;
        }

        new Thread(() -> {
            Auction latest = auctionService.getAuctionById(auctionId);
            Platform.runLater(() -> {
                auction = latest;
                AutoBidManager.getInstance().updateAuction(auctionId, auction);

                if (auction == null) { showMsg("Auction not available", false); return; }
                // FIX: check cả endTime lẫn status — tránh trường hợp DB chưa update kịp
                if (LocalDateTime.now().isAfter(auction.getEndTime())) {
                    auction.setStatus(AuctionStatus.CLOSED);
                    updateAuctionStatus();
                    timerLabel.setText("Ended");
                    showMsg("Auction has ended!", false);
                    return;
                }
                if (auction.getStatus() != AuctionStatus.OPEN)   { updateAuctionStatus(); showMsg("Auction is not open!", false); return; }
                if (amount.compareTo(auction.getCurrentPrice()) <= 0) { showMsg("Bid must be higher than " + fmt(auction.getCurrentPrice()), false); return; }

                currentPrice.setText(fmt(amount));
                auction.setCurrentPrice(amount);

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

    // ── Socket listener ───────────────────────────────────────────────────────

    @Override
    public void onRespone(JsonObject response) {
        if (!response.has("action")) return;
        String action = response.get("action").getAsString();

        if ("AUCTION_SETTLED".equals(action)) {
            com.btl.n8.DTO.AuctionSettledResponse settled =
                    gson.fromJson(response, com.btl.n8.DTO.AuctionSettledResponse.class);
            if (settled.getAuctionId() != this.auctionId) return;

            Platform.runLater(() -> {
                if (countdownTimer != null) countdownTimer.cancel();
                bidInput.setDisable(true);
                timerLabel.setText("Ended");
                if (auction != null) {
                    auction.setStatus(AuctionStatus.CLOSED);
                    updateAuctionStatus();
                }
                new Thread(() -> {
                    try {
                        java.sql.Connection conn = com.btl.n8.Connection.DataConnection.getConnection();
                        if (conn != null) {
                            com.btl.n8.Model.Entity.User updated =
                                    new com.btl.n8.Connection.UserDAOImpl(conn)
                                            .findById(SessionManager.getInstance().getCurrentUser().getId());
                            if (updated != null) SessionManager.getInstance().setCurrentUser(updated);
                        }
                    } catch (Exception ignored) {}
                }).start();

                if (settled.getWinnerId() == -1) {
                    showSettledDialog("Ph\u00eci\u00ean \u0111\u1ea5u gi\u00e1 k\u1ebft th\u00fac",
                            "Kh\u00f4ng c\u00f3 ng\u01b0\u1eddi tham gia \u0111\u1eb7t gi\u00e1.", false);
                } else if (settled.isWinner()) {
                    showSettledDialog("\ud83c\udfc6 B\u1ea1n \u0111\u00e3 th\u1eafng!",
                            String.format("Ch\u00fac m\u1eebng! B\u1ea1n th\u1eafng v\u1edbi gi\u00e1 %s.\nBalance \u0111\u00e3 b\u1ecb tr\u1eeb t\u1ef1 \u0111\u1ed9ng.",
                                    fmt(settled.getWinningPrice())), true);
                } else {
                    showSettledDialog("Ph\u00eci\u00ean \u0111\u1ea5u gi\u00e1 k\u1ebft th\u00fac",
                            String.format("Ng\u01b0\u1eddi th\u1eafng: %s\nGi\u00e1 th\u1eafng: %s",
                                    settled.getWinnerAccount(),
                                    fmt(settled.getWinningPrice())), false);
                }
            });
            return;
        }

        // \u2500\u2500 BID_UPDATE \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500
        if (!"BID_UPDATE".equals(action)) return;

        BidResponse res = gson.fromJson(response, BidResponse.class);
        if (res.getAuctionId() != this.auctionId) return;

        Platform.runLater(() -> {
            if (res.isSuccess()) {
                currentPrice.setText(fmt(res.getCurrentPrice()));
                if (auction != null) {
                    auction.setCurrentPrice(res.getCurrentPrice());
                    AutoBidManager.getInstance().updateAuction(auctionId, auction);
                }

                // Anti-sniping: server gia hạn endTime → reset countdown
                if (res.getNewEndTime() != null && auction != null) {
                    auction.setEndTime(res.getNewEndTime());
                    AutoBidManager.getInstance().updateAuction(auctionId, auction);
                    if (countdownTimer != null) countdownTimer.cancel();
                    startCountdown(res.getNewEndTime());
                    showMsg("⏰ Anti-snipe! Gia hạn thêm " + SNIPE_EXTENSION_SECONDS + "s", true);
                } else {
                    showMsg(res.getBidderId() != bidderId
                            ? "⚡ Someone placed: " + fmt(res.getCurrentPrice())
                            : "✓ Your bid was placed!", true);
                }
                if (bidService != null) loadChart();
            } else {
                if (auction != null) {
                    currentPrice.setText(fmt(res.getCurrentPrice()));
                    auction.setCurrentPrice(res.getCurrentPrice());
                }
                String msg = res.getMessage() != null ? res.getMessage() : "Bid failed";
                showMsg(msg, false);
                // FIX: nếu server báo auction đã kết thúc → disable UI ngay,
                // không chờ countdown (có thể đã miss do popup mở từ trước)
                if (msg.contains("kết thúc") || msg.contains("đóng") || msg.contains("ended") || msg.contains("closed")) {
                    bidInput.setDisable(true);
                    timerLabel.setText("Ended");
                    if (auction != null) {
                        auction.setStatus(AuctionStatus.CLOSED);
                        updateAuctionStatus();
                    }
                }
            }
        });
    }

    // ── Countdown ─────────────────────────────────────────────────────────────

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
                        // FIX: cập nhật status local ngay lập tức — không chờ DB reload.
                        // Ngăn handlePlaceBid() check thấy OPEN trong khoảng trống trước khi DB trả về.
                        if (auction != null) {
                            auction.setStatus(AuctionStatus.CLOSED);
                            updateAuctionStatus();
                        }
                        showMsg("Auction has ended!", false);
                    });
                    // Reload DB trong background để đồng bộ status thật từ server
                    new Thread(() -> {
                        if (auctionService != null) {
                            Auction updated = auctionService.getAuctionById(auctionId);
                            Platform.runLater(() -> {
                                auction = updated;
                                AutoBidManager.getInstance().updateAuction(auctionId, auction);
                                updateAuctionStatus();
                            });
                        }
                    }).start();
                    countdownTimer.cancel();
                    return;
                }
                long h = diff / 3600, m = (diff % 3600) / 60, s = diff % 60;
                Platform.runLater(() -> timerLabel.setText(String.format("%02d:%02d:%02d", h, m, s)));
            }
        }, 0, 1000);
    }

    // ── AutoBid popup ─────────────────────────────────────────────────────────

    @FXML
    public void handleOpenAutoBid() {
        // Popup đang mở → focus lại
        if (autoBidStage != null && autoBidStage.isShowing()) {
            autoBidStage.requestFocus();
            return;
        }

        // Mở popup mới (dù AutoBid đang active hay không đều load FXML)
        // AutoBidController.initData() sẽ tự detect và restore UI nếu active
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AutoBid.fxml"));
            Parent root = loader.load();

            autoBidCtrl = loader.getController();
            autoBidCtrl.initData(auctionId, item, auction);

            // Callback để cập nhật nút khi AutoBid active/inactive
            autoBidCtrl.setOnActiveChanged(active -> Platform.runLater(() -> {
                if (active) setAutoBidButtonActive();
                else        setAutoBidButtonDefault();
            }));

            autoBidStage = new Stage();
            autoBidStage.initStyle(StageStyle.UNDECORATED);
            autoBidStage.setTitle("AutoBid");
            autoBidStage.setScene(new Scene(root));
            autoBidStage.initModality(Modality.NONE);
            autoBidStage.setResizable(false);

            autoBidStage.setOnHidden(e -> {
                // Hủy UI callback của popup — AutoBidManager session vẫn sống
                if (autoBidCtrl != null) autoBidCtrl.detachUI();
                autoBidCtrl  = null;
                autoBidStage = null;

                // Đăng ký lại callback cho BidDetailController
                // để nút vẫn phản ánh đúng khi popup đóng
                AutoBidManager.getInstance().setUICallback(auctionId, this::handleAutoBidManagerEvent);

                // Sync nút ngay với trạng thái thực tế của Manager
                if (AutoBidManager.getInstance().isActive(auctionId)) setAutoBidButtonActive();
                else                                                    setAutoBidButtonDefault();
            });

            autoBidStage.show();

        } catch (Exception ex) {
            showMsg("Failed to open AutoBid: " + ex.getMessage(), false);
            ex.printStackTrace();
        }
    }

    // ── Nút AutoBid ───────────────────────────────────────────────────────────

    private void setAutoBidButtonActive() {
        autoBidButton.setText("⚡ ON — Cancel");
        autoBidButton.setStyle(STYLE_ACTIVE);
        autoBidButton.setOnAction(ev -> AutoBidManager.getInstance().cancel(auctionId));
    }

    private void setAutoBidButtonDefault() {
        autoBidButton.setText("⚡ AutoBid");
        autoBidButton.setStyle(STYLE_DEFAULT);
        autoBidButton.setOnAction(ev -> handleOpenAutoBid());
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Gọi từ BidController.setOnHidden khi bidDetails đóng.
     * Dọn timer, socket listener của bidDetails — KHÔNG đụng vào AutoBidManager.
     */
    public void cleanup() {
        if (countdownTimer != null) countdownTimer.cancel();
        ClientSocket.getInstance().removeListener(this);

        // Hủy UI callback của bidDetails với Manager (AutoBidManager session vẫn sống)
        AutoBidManager.getInstance().setUICallback(auctionId, null);

        // Hủy UI callback của popup nếu còn mở
        if (autoBidCtrl != null) autoBidCtrl.detachUI();
        autoBidCtrl  = null;
        autoBidStage = null;
    }

    @FXML
    public void handleClose() {
        cleanup();
        Stage stage = (Stage) bidInput.getScene().getWindow();
        stage.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showMsg(String msg, boolean success) {
        bidMsg.setStyle(success
                ? "-fx-text-fill: #00ff88; -fx-font-size: 12px;"
                : "-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
        bidMsg.setText(msg);
    }

    private void showSettledDialog(String title, String content, boolean isWinner) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                isWinner ? javafx.scene.control.Alert.AlertType.INFORMATION
                        : javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String fmt(BigDecimal n) {
        return String.format("%,.0f ₫", n);
    }
}