package com.btl.n8.Controller;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.*;
import com.btl.n8.Service.BidService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class accounthomeController {

    @FXML private Label avatarLabel;
    @FXML private Label usernameLabel;
    @FXML private Label usernameLabel2;
    @FXML private Label idLabel;
    @FXML private Label roleLabel;
    @FXML private Label roleLabel2;

    @FXML private TableView<BidRow> bidTable;
    @FXML private TableColumn<BidRow, String> colItem;
    @FXML private TableColumn<BidRow, String> colAmount;
    @FXML private TableColumn<BidRow, String> colTime;
    @FXML private TableColumn<BidRow, String> colStatus;

    private ObservableList<BidRow> bidRows = FXCollections.observableArrayList();
    private BidService bidService;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            usernameLabel.setText("Not logged in");
            return;
        }

        // Avatar — 2 chữ đầu username
        String account = user.getAccount();
        String initials = account.length() >= 2
                ? account.substring(0, 2).toUpperCase()
                : account.toUpperCase();
        avatarLabel.setText(initials);

        // Thông tin user
        usernameLabel.setText(account);
        usernameLabel2.setText(account);
        idLabel.setText("#" + user.getId());
        roleLabel.setText(user.getRole().name());
        roleLabel2.setText(user.getRole().name());

        // Màu role badge
        switch (user.getRole()) {
            case BIDDER -> roleLabel.setStyle(
                    "-fx-background-color: #1a3a5c; -fx-text-fill: #7ecfff; " +
                            "-fx-padding: 3 10 3 10; -fx-background-radius: 99;");
            case SELLER -> roleLabel.setStyle(
                    "-fx-background-color: #3a2800; -fx-text-fill: #ffd700; " +
                            "-fx-padding: 3 10 3 10; -fx-background-radius: 99;");
            case ADMIN -> roleLabel.setStyle(
                    "-fx-background-color: #2a003a; -fx-text-fill: #cc88ff; " +
                            "-fx-padding: 3 10 3 10; -fx-background-radius: 99;");
        }

        // Setup table
        colItem.setCellValueFactory(new PropertyValueFactory<>("item"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        bidTable.setItems(bidRows);

        // Load bid history
        loadBidHistory(user.getId());
    }

    private void loadBidHistory(int userId) {
        new Thread(() -> {
            try {
                Connection conn = DataConnection.getConnection();
                if (conn == null) return;

                bidService = new BidService(new BidDAOImpl(conn));
                // Lấy tất cả bid của user — cần thêm method vào BidDAO nếu chưa có
                List<Bid> bids = bidService.getBidsByBidder(userId);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM");

                for (Bid bid : bids) {
                    String time   = bid.getBidTime() != null ? bid.getBidTime().format(fmt) : "-";
                    String amount = String.format("%,.0f ₫", bid.getAmount());
                    bidRows.add(new BidRow(
                            "#" + bid.getAuctionId(),
                            amount,
                            time,
                            bid.getStatus().name()
                    ));
                }

                Platform.runLater(() -> bidTable.setItems(bidRows));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleClose() {
        // Đóng popup
        Stage stage = (Stage) avatarLabel.getScene().getWindow();
        stage.close();
    }

    public static class BidRow {
        private final String item, amount, time, status;

        public BidRow(String item, String amount, String time, String status) {
            this.item = item; this.amount = amount;
            this.time = time; this.status = status;
        }

        public String getItem()   { return item; }
        public String getAmount() { return amount; }
        public String getTime()   { return time; }
        public String getStatus() { return status; }
    }
}