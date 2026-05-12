package com.btl.n8.Controller;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.*;
import com.btl.n8.Service.AuctionService;
import com.btl.n8.Service.BidService;
import com.btl.n8.dto.BidRequest;
import com.btl.n8.dto.BidResponse;
import com.btl.n8.network.ClientSocket;
import com.btl.n8.network.ServerResponseListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
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

    private int auctionId;
    private int bidderId;
    private Auction auction;
    private Timer countdownTimer;
    private static final Gson gson = new Gson();

    private AuctionService auctionService;
    private BidService bidService;

    public void initData(int auctionId, Item item) {
        this.auctionId = auctionId;
        this.bidderId  = SessionManager.getInstance().getCurrentUser().getId();

        try {
            Connection conn = DataConnection.getConnection();
            if (conn == null) throw new Exception("Database connection failed");

            auctionService = new AuctionService(new AuctionDAOImpl(conn));
            bidService     = new BidService(new BidDAOImpl(conn));
            auction        = auctionService.getAuctionById(auctionId);

        } catch (Exception e) {
            showMsg("Failed to load data. Please try again.", false);
            e.printStackTrace();
            return;
        }

        // Điền thông tin item
        itemName.setText(item.getName());
        itemId.setText("#" + item.getId());
        itemType.setText(item.getType().name());
        updateAuctionStatus();

        // Hiển thị ảnh nếu có
        if (item.getImage() != null && item.getImage().length > 0) {
            try {
                Image img = new Image(new java.io.ByteArrayInputStream(item.getImage()));
                itemImage.setImage(img);
            } catch (Exception e) {
                System.out.println("Không load được ảnh item");
            }
        }

        loadChart();
        if (auction != null) startCountdown(auction.getEndTime());

        // Đăng ký Observer — lắng nghe broadcast từ server
        ClientSocket.getInstance().addListener(this);
    }

    private void updateAuctionStatus() {
        if (auction == null) {
            itemStatus.setText("-");
            currentPrice.setText("-");
            return;
        }

        currentPrice.setText(fmt(auction.getCurrentPrice()));
        String status = auction.getStatus().name();
        itemStatus.setText(status);

        switch (auction.getStatus()) {
            case OPEN -> itemStatus.setStyle(
                    "-fx-background-color: #00ff88; -fx-text-fill: #0a1628; " +
                            "-fx-padding: 2 8 2 8; -fx-background-radius: 99;");
            case CLOSED -> itemStatus.setStyle(
                    "-fx-background-color: #888888; -fx-text-fill: white; " +
                            "-fx-padding: 2 8 2 8; -fx-background-radius: 99;");
            case CANCELLED -> itemStatus.setStyle(
                    "-fx-background-color: #ff6b6b; -fx-text-fill: white; " +
                            "-fx-padding: 2 8 2 8; -fx-background-radius: 99;");
        }
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

        if (auction == null)                            { showMsg("Auction not available", false); return; }
        if (auction.getStatus() != AuctionStatus.OPEN)  { showMsg("Auction is not open", false); return; }
        if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
            showMsg("Bid must be higher than " + fmt(auction.getCurrentPrice()), false);
            return;
        }

        // Gửi BidRequest qua Socket — server xử lý + broadcast
        new Thread(() -> {
            BidRequest req = new BidRequest(auctionId, bidderId, amount);
            req.setSessionId(SessionManager.getInstance().getSessionId());
            ClientSocket.getInstance().sendMessage(req);
        }).start();

        bidInput.clear();
        showMsg("Bid sent...", true);
    }

    // Observer: nhận broadcast BID_UPDATE từ server
    @Override
    public void onRespone(JsonObject response) {
        String action = response.get("action").getAsString();
        if (!"BID_UPDATE".equals(action)) return; // khớp với BidResponse constructor

        BidResponse res = gson.fromJson(response, BidResponse.class);
        if (res.getAuctionId() != this.auctionId) return; // không phải auction đang xem

        Platform.runLater(() -> {
            if (res.isSuccess()) {
                // Reload auction từ DB để lấy trạng thái mới nhất
                new Thread(() -> {
                    Auction updated = auctionService.getAuctionById(auctionId);
                    Platform.runLater(() -> {
                        auction = updated;
                        updateAuctionStatus();
                        loadChart();
                        showMsg("New bid: " + fmt(res.getCurrentPrice()), true);
                    });
                }).start();
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
                        // Fix: reload auction để lấy status mới nhất từ DB
                        new Thread(() -> {
                            Auction updated = auctionService.getAuctionById(auctionId);
                            Platform.runLater(() -> {
                                auction = updated;
                                updateAuctionStatus();
                                bidInput.setDisable(true);
                                showMsg("Auction has ended!", false);
                            });
                        }).start();
                    });
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

    @FXML
    public void handleClose() {
        if (countdownTimer != null) countdownTimer.cancel();
        ClientSocket.getInstance().removeListener(this); // tránh memory leak
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