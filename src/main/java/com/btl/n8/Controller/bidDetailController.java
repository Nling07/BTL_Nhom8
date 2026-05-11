package com.btl.n8.Controller;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.*;
import com.btl.n8.Service.AuctionService;
import com.btl.n8.Service.BidService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class bidDetailController {

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

        itemName.setText(item.getName());
        itemId.setText("#" + item.getId());
        itemType.setText(item.getType().name());
        itemStatus.setText(auction != null ? auction.getStatus().name() : "-");
        currentPrice.setText(fmt(auction != null ? auction.getCurrentPrice() : BigDecimal.ZERO));

        loadChart();
        if (auction != null) startCountdown(auction.getEndTime());
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

        if (text.isEmpty()) {
            showMsg("Please enter a bid amount", false);
            return;
        }

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

        if (auction == null) {
            showMsg("Auction not available", false);
            return;
        }
        if (auction.getStatus() != AuctionStatus.OPEN) {
            showMsg("Auction is not open", false);
            return;
        }
        if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
            showMsg("Bid must be higher than " + fmt(auction.getCurrentPrice()), false);
            return;
        }

        new Thread(() -> {
            boolean ok = bidService.placeBid(auctionId, bidderId, amount);
            Platform.runLater(() -> {
                if (ok) {
                    auction = auctionService.getAuctionById(auctionId);
                    currentPrice.setText(fmt(auction.getCurrentPrice()));
                    bidInput.clear();
                    showMsg("Bid placed!", true);
                    loadChart();
                } else {
                    showMsg("Failed to place bid", false);
                }
            });
        }).start();
    }

    private void startCountdown(LocalDateTime endTime) {
        countdownTimer = new Timer(true);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long diff = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
                if (diff <= 0) {
                    Platform.runLater(() -> timerLabel.setText("Ended"));
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