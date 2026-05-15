package com.btl.n8.Controller;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.entity.Auction;
import com.btl.n8.Model.entity.Bid;
import com.btl.n8.Model.entity.Item;
import com.btl.n8.Model.enums.AuctionStatus;
import com.btl.n8.Service.AuctionService;
import com.btl.n8.Service.BidService;
import com.btl.n8.dto.BidRequest;
import com.btl.n8.dto.BidResponse;
import com.btl.n8.network.ClientSocket;
import com.btl.n8.network.ServerResponseListener;
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
import com.btl.n8.util.LocalDateTimeAdapter;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class bidDetailController implements ServerResponseListener {

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
    private Item item;   // keep reference for AutoBid popup
    private Timer countdownTimer;
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private AuctionService auctionService;
    private BidService bidService;

    /**
     * Được gọi từ bidController (trên FX thread) ngay sau loader.load().
     *
     * FIX BUG #1: Tách initData thành 2 bước:
     *   1. Set các field từ Item (không cần DB) ngay lập tức trên FX thread.
     *   2. Chạy toàn bộ DB call trong background thread, chỉ update UI
     *      trong Platform.runLater() khi có kết quả.
     *
     * Trước đây toàn bộ DataConnection.getConnection() + getAuctionById()
     * chạy trên FX thread → UI đơ.
     */
    public void initData(int auctionId, Item item) {
        this.auctionId = auctionId;
        this.bidderId  = SessionManager.getInstance().getCurrentUser().getId();
        this.item      = item;   // store for AutoBid popup

        // ── Bước 1: Set UI từ Item ngay (không cần DB, không block) ──────────
        itemName.setText(item.getName());
        itemId.setText("#" + item.getId());
        itemType.setText(item.getType().name());
        timerLabel.setText("Loading...");
        currentPrice.setText("Loading...");
        itemStatus.setText("-");
        bidInput.setDisable(true); // disable cho đến khi load xong

        if (item.getImage() != null && item.getImage().length > 0) {
            try {
                Image img = new Image(new java.io.ByteArrayInputStream(item.getImage()));
                itemImage.setImage(img);
            } catch (Exception e) {
                System.out.println("Không load được ảnh item");
            }
        }

        // Đăng ký lắng nghe broadcast ngay (trước khi load DB xong)
        ClientSocket.getInstance().addListener(this);

        // ── Bước 2: Load DB trong background thread ───────────────────────────
        new Thread(() -> {
            try {
                Connection conn = DataConnection.getConnection();
                if (conn == null) throw new Exception("Database connection failed");

                auctionService = new AuctionService(new AuctionDAOImpl(conn));
                bidService     = new BidService(new BidDAOImpl(conn));
                Auction loaded = auctionService.getAuctionById(auctionId);

                // Kết quả DB → về FX thread để update UI
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

        // Reload auction mới nhất trước khi check
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

                // Gửi qua Socket
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

    /**
     * Nhận broadcast BID_UPDATE từ server.
     *
     * FIX: BidResponse đã chứa currentPrice mới do server tính và gửi về.
     * Version cũ bỏ qua giá đó, lại gọi thêm auctionService.getAuctionById()
     * (1 DB roundtrip nữa) → delay, hoặc crash NullPointerException nếu
     * auctionService chưa init xong (race condition với initData background thread).
     *
     * Fix: dùng res.getCurrentPrice() trực tiếp để update label ngay lập tức,
     * sau đó mới loadChart() trong background (không block UI).
     * Không cần DB call trong onRespone nữa.
     */
    @Override
    public void onRespone(JsonObject response) {
        if (!response.has("action")) return;
        String action = response.get("action").getAsString();
        if (!"BID_UPDATE".equals(action)) return;

        BidResponse res = gson.fromJson(response, BidResponse.class);
        if (res.getAuctionId() != this.auctionId) return;

        Platform.runLater(() -> {
            if (res.isSuccess()) {
                // Cập nhật giá ngay lập tức từ data có sẵn trong response
                // Không cần DB call → không delay, không NPE
                currentPrice.setText(fmt(res.getCurrentPrice()));

                // Cập nhật object auction local để handlePlaceBid check đúng giá
                if (auction != null) {
                    auction.setCurrentPrice(res.getCurrentPrice());
                }

                // Thông báo
                if (res.getBidderId() != bidderId) {
                    showMsg("⚡ Someone placed: " + fmt(res.getCurrentPrice()), true);
                } else {
                    showMsg("✓ Your bid was placed!", true);
                }

                // Reload chart trong background (không block UI)
                if (bidService != null) {
                    loadChart();
                }

            } else {
                showMsg(res.getMessage(), false);
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

    /**
     * Dọn dẹp resource: cancel timer + remove socket listener.
     * Gọi từ handleClose() VÀ từ stage.setOnHidden() trong bidController
     * để đảm bảo cleanup dù popup đóng bằng cách nào.
     */
    public void cleanup() {
        if (countdownTimer != null) countdownTimer.cancel();
        ClientSocket.getInstance().removeListener(this);
    }

    /**
     * Mở popup AutoBid khi người dùng nhấn nút "⚡ AutoBid".
     */
    @FXML
    public void handleOpenAutoBid() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AutoBid.fxml"));
            Parent root = loader.load();

            autoBidController controller = loader.getController();
            controller.initData(auctionId, item, auction);

            Stage popup = new Stage();
            popup.initStyle(StageStyle.UNDECORATED);
            popup.setTitle("AutoBid");
            popup.setScene(new Scene(root));
            popup.initModality(Modality.NONE); // non-modal so bid detail stays usable
            popup.setResizable(false);

            popup.setOnHidden(e -> controller.cleanup());
            popup.show();

        } catch (Exception ex) {
            showMsg("Failed to open AutoBid: " + ex.getMessage(), false);
            ex.printStackTrace();
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