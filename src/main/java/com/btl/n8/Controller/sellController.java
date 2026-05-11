package com.btl.n8.Controller;

import com.btl.n8.Connection.*;
import com.btl.n8.DTO.AddItemRequest;
import com.btl.n8.Model.*;
import com.btl.n8.Service.AuctionService;
import com.btl.n8.util.FileUtils;
import com.btl.n8.Service.ItemService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class sellController {

    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField priceField;
    @FXML private Label uploadLabel;
    @FXML private Label messageLabel;

    @FXML private TableView<SellRow> myItemTable;
    @FXML private TableColumn<SellRow, Integer> colId;
    @FXML private TableColumn<SellRow, String>  colName;
    @FXML private TableColumn<SellRow, String>  colType;
    @FXML private TableColumn<SellRow, String>  colStartPrice;
    @FXML private TableColumn<SellRow, String>  colCurrentPrice;
    @FXML private TableColumn<SellRow, String>  colStatus;

    private File selectedImage;
    private ObservableList<SellRow> myItems = FXCollections.observableArrayList();
    private int sellerId;

    private ItemService itemService;
    private AuctionService auctionService;

    @FXML
    public void initialize() {
        sellerId = SessionManager.getInstance().getCurrentUser().getId();
        typeCombo.setItems(FXCollections.observableArrayList("POSTER", "FIGURE", "CARD"));

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        myItemTable.setItems(myItems);

        new Thread(() -> {
            try {
                Connection conn = DataConnection.getConnection();
                if (conn == null) throw new Exception("Database connection failed");

                itemService    = new ItemService(new ItemDAOImpl(conn));
                auctionService = new AuctionService(new AuctionDAOImpl(conn));

                List<Item> items = itemService.getItemsBySeller(sellerId);
                List<SellRow> rows = buildRows(items);

                Platform.runLater(() -> {
                    myItems.clear();
                    myItems.addAll(rows);
                });

            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load data"));
                e.printStackTrace();
            }
        }).start();
    }

    // Helper — build rows trên background thread
    private List<SellRow> buildRows(List<Item> items) {
        List<SellRow> rows = new ArrayList<>();
        for (Item item : items) {
            Auction auction     = auctionService.getAuctionByItemId(item.getId());
            String startPrice   = auction != null ? fmt(auction.getStartingPrice()) : "-";
            String currentPrice = auction != null ? fmt(auction.getCurrentPrice())  : "-";
            String status       = auction != null ? auction.getStatus().name()      : "NO AUCTION";
            rows.add(new SellRow(
                    item.getId(), item.getName(), item.getType().name(),
                    startPrice, currentPrice, status
            ));
        }
        return rows;
    }

    // Reload bảng sau khi đăng item — chạy trên background thread
    private void reloadTable() {
        new Thread(() -> {
            List<Item> items = itemService.getItemsBySeller(sellerId);
            List<SellRow> rows = buildRows(items);
            Platform.runLater(() -> {
                myItems.clear();
                myItems.addAll(rows);
            });
        }).start();
    }

    @FXML
    public void handleUpload(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select product image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        selectedImage = fc.showOpenDialog(stage);

        if (selectedImage != null) {
            uploadLabel.setText("✓ " + selectedImage.getName());
        }
    }

    @FXML
    public void handleSell(ActionEvent event) {
        messageLabel.setText("");
        messageLabel.setStyle("");

        String name      = nameField.getText().trim();
        String type      = typeCombo.getValue();
        String priceText = priceField.getText().trim();

        if (name.isEmpty())        { showError("Please enter item name"); return; }
        if (type == null)          { showError("Please select item type"); return; }
        if (priceText.isEmpty())   { showError("Please enter starting price"); return; }
        if (selectedImage == null) { showError("Please select an image"); return; }

        BigDecimal startingPrice;
        try {
            startingPrice = new BigDecimal(priceText);
            if (startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Price must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Invalid price format");
            return;
        }

        Button submitBtn = (Button) event.getSource();
        submitBtn.setDisable(true);

        new Thread(() -> {
            try {
                byte[] imageBytes = FileUtils.toByteArray(selectedImage);
                if (imageBytes == null) {
                    Platform.runLater(() -> {
                        showError("Error reading image file");
                        submitBtn.setDisable(false);
                    });
                    return;
                }

                Item item = itemService.createItem(name, type, sellerId, imageBytes);
                boolean itemOk = itemService.addItem(item);
                if (!itemOk) {
                    Platform.runLater(() -> {
                        showError("Failed to create item");
                        submitBtn.setDisable(false);
                    });
                    return;
                }

                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime   = startTime.plusDays(2);

                Auction auction = new Auction(
                        0, item.getId(),
                        startingPrice, startingPrice,
                        startTime, endTime,
                        AuctionStatus.OPEN
                );

                boolean auctionOk = auctionService.createAuction(auction);
                if (!auctionOk) {
                    Platform.runLater(() -> {
                        showError("Failed to create auction");
                        submitBtn.setDisable(false);
                    });
                    return;
                }

                // Reset form trên UI thread
                Platform.runLater(() -> {
                    nameField.clear();
                    typeCombo.setValue(null);
                    priceField.clear();
                    uploadLabel.setText("");
                    selectedImage = null;
                    showSuccess("Item listed successfully!");
                    submitBtn.setDisable(false);
                });

                // Reload bảng trên background thread — không block UI
                reloadTable();

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error: " + e.getMessage());
                    submitBtn.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
        //AddItemRequest addItemRequest = new AddItemRequest(name,sessionId,type,startingPrice,selectedImage,startTime,EndTime)
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #ff6b6b;");
        messageLabel.setText(message);
    }

    private void showSuccess(String message) {
        messageLabel.setStyle("-fx-text-fill: #00ff88;");
        messageLabel.setText(message);
    }

    @FXML
    public void Return(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/home.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    private String fmt(BigDecimal n) {
        return String.format("%,.0f ₫", n);
    }

    public static class SellRow {
        private final int id;
        private final String name, type, startPrice, currentPrice, status;

        public SellRow(int id, String name, String type,
                       String startPrice, String currentPrice, String status) {
            this.id = id; this.name = name; this.type = type;
            this.startPrice = startPrice; this.currentPrice = currentPrice;
            this.status = status;
        }

        public int    getId()           { return id; }
        public String getName()         { return name; }
        public String getType()         { return type; }
        public String getStartPrice()   { return startPrice; }
        public String getCurrentPrice() { return currentPrice; }
        public String getStatus()       { return status; }
    }
}