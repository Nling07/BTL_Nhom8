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
import javafx.scene.control.Alert;
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
    // FIX: dùng closeBtn (thay vì bidInput) để lấy window trong handleClose
    @FXML private Button closeBtn;

    private int auctionId;
    private int bidderId;
    private Auction auction;
    private Item item;
    private Timer countdownTimer;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private static final int SNIPE_EXTENSION_SECONDS = 30;

    private AuctionService auctionService;
    private BidService bidService;

    // ── AutoBid popup state ───────────────────────────────────────────────────
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

        AutoBidManager.getInstance().setUICallback(auctionId, this::handleAutoBidManagerEvent);

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

    private void handleAutoBidManagerEvent(String event) {
        if (event == null) return;
        String[] parts = event.split(":", 2);
        String   type  = parts[0];
        String   msg   = parts.length > 1 ? parts[1] : "";

        switch (type) {
            case "CANCELLED" -> {
                setAutoBidButtonDefault();
                showMsg("AutoBid đã huỷ.", false);
            }
            case "STOPPED" -> {
                setAutoBidButtonDefault();
                // Hiện lý do dừng ra màn hình chính để user biết
                showMsg("🤖 " + (msg.isEmpty() ? "AutoBid đã dừng." : msg), false);
            }
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

    /**
     * FIX: bids đã được query theo bid_time ASC nên dùng thẳng thứ tự đó,
     * không đảo ngược vòng lặp nữa → chart hiển thị đúng tiến trình theo thời gian.
     */
    private void loadChart() {
        new Thread(() -> {
            List<Bid> bids = bidService.getBidsByAuction(auctionId);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Price");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
            // FIX: duyệt thuận chiều (bid_time ASC từ DB) — không đảo ngược
            for (Bid bid : bids) {
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
                if (LocalDateTime.now().isAfter(auction.getEndTime())) {
                    auction.setStatus(AuctionStatus.CLOSED);
                    updateAuctionStatus();
                    timerLabel.setText("Ended");
                    showMsg("Auction has ended!", false);
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

                // Kiểm tra balance phía client (nhanh, tránh gửi bid không cần thiết)
                com.btl.n8.Model.Entity.User me = SessionManager.getInstance().getCurrentUser();
                java.math.BigDecimal myBalance = (me != null && me.getBalance() != null)
                        ? me.getBalance() : java.math.BigDecimal.ZERO;
                if (amount.compareTo(myBalance) > 0) {
                    showMsg(String.format("Số dư không đủ! Balance: %s — Cần: %s",
                            fmt(myBalance), fmt(amount)), false);
                    return;
                }

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

                // Cập nhật balance trong SessionManager:
                // - Nếu thắng: server gửi newBalance mới → dùng luôn, không cần query DB
                // - Nếu thua: balance không thay đổi, không cần làm gì
                if (settled.isWinner() && settled.getNewBalance() != null) {
                    com.btl.n8.Model.Entity.User me = SessionManager.getInstance().getCurrentUser();
                    if (me != null) {
                        me.setBalance(settled.getNewBalance());
                        SessionManager.getInstance().setCurrentUser(me);
                    }
                } else if (!settled.isWinner()) {
                    // Reload user từ DB trên background thread (không block UI)
                    new Thread(() -> {
                        try {
                            com.btl.n8.Model.Entity.User updated =
                                    new UserDAOImpl(DataConnection.getConnection())
                                            .findById(SessionManager.getInstance().getCurrentUser().getId());
                            if (updated != null) {
                                Platform.runLater(() ->
                                        SessionManager.getInstance().setCurrentUser(updated));
                            }
                        } catch (Exception ignored) {}
                    }).start();
                }

                if (settled.getWinnerId() == -1) {
                    showSettledDialog("Phiên đấu giá kết thúc",
                            "Không có người tham gia đặt giá.", false);
                } else if (settled.isWinner()) {
                    showSettledDialog("🏆 Bạn đã thắng!",
                            String.format("Chúc mừng! Bạn thắng với giá %s.\nBalance đã bị trừ tự động.",
                                    fmt(settled.getWinningPrice())), true);
                } else {
                    showSettledDialog("Phiên đấu giá kết thúc",
                            String.format("Người thắng: %s\nGiá thắng: %s",
                                    settled.getWinnerAccount(),
                                    fmt(settled.getWinningPrice())), false);
                }
            });
            return;
        }

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
                        if (auction != null) {
                            auction.setStatus(AuctionStatus.CLOSED);
                            updateAuctionStatus();
                        }
                        showMsg("Auction has ended!", false);
                    });
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
        if (autoBidStage != null && autoBidStage.isShowing()) {
            autoBidStage.requestFocus();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AutoBid.fxml"));
            Parent root = loader.load();

            autoBidCtrl = loader.getController();
            autoBidCtrl.initData(auctionId, item, auction);

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
                if (autoBidCtrl != null) autoBidCtrl.detachUI();
                autoBidCtrl  = null;
                autoBidStage = null;

                AutoBidManager.getInstance().setUICallback(auctionId, this::handleAutoBidManagerEvent);

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

    public void cleanup() {
        if (countdownTimer != null) countdownTimer.cancel();
        ClientSocket.getInstance().removeListener(this);
        AutoBidManager.getInstance().setUICallback(auctionId, null);
        if (autoBidCtrl != null) autoBidCtrl.detachUI();
        autoBidCtrl  = null;
        autoBidStage = null;
    }

    @FXML
    public void handleClose() {
        cleanup();
        // FIX: dùng closeBtn để lấy Stage (nhất quán với fx:id trong FXML)
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showMsg(String msg, boolean success) {
        bidMsg.setStyle(success
                ? "-fx-text-fill: #00ff88; -fx-font-size: 12px;"
                : "-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
        bidMsg.setText(msg);
    }

    /**
     * FIX: dùng đúng AlertType theo kết quả —
     * INFORMATION cho thắng, WARNING cho thua/không có người bid.
     */
    private void showSettledDialog(String title, String content, boolean isWinner) {
        Alert alert = new Alert(
                isWinner ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String fmt(BigDecimal n) {
        return String.format("%,.0f ₫", n);
    }
}