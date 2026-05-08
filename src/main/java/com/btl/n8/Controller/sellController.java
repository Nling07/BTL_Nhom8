package com.btl.n8.Controller;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.*;
import com.btl.n8.Service.AuctionService;
import com.btl.n8.Service.ItemService;
import com.btl.n8.Service.FileUtils;

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
        sellerId = SessionManager.getCurrentUser().getId();
        typeCombo.setItems(FXCollections.observableArrayList("POSTER", "FIGURE", "CARD"));

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        myItemTable.setItems(myItems);

        new Thread(() -> {
            try (Connection conn = DataConnection.getConnection()) {
                itemService    = new ItemService(new ItemDAOImpl(conn));
                auctionService = new AuctionService(new AuctionDAOImpl(conn));

                Platform.runLater(() -> loadMyItems());
            } catch (Exception e) {
                Platform.runLater(() -> {
                    messageLabel.setText("Failed to load data");
                    messageLabel.setVisible(true);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void loadMyItems() {
        myItems.clear();
        List<Item> items = itemService.getItemsBySeller(sellerId);

        for (Item item : items) {
            Auction auction     = auctionService.getAuctionByItemId(item.getId());
            String startPrice   = auction != null ? fmt(auction.getStartingPrice()) : "-";
            String currentPrice = auction != null ? fmt(auction.getCurrentPrice())  : "-";
            String status       = auction != null ? auction.getStatus().name()      : "NO AUCTION";

            myItems.add(new SellRow(
                    item.getId(),
                    item.getName(),
                    item.getType().name(),
                    startPrice,
                    currentPrice,
                    status
            ));
        }
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
            uploadLabel.setText("ok " + selectedImage.getName());
        }
    }

    @FXML
    public void handleSell(ActionEvent event) {
        messageLabel.setText("");
        messageLabel.setStyle("");

        String name      = nameField.getText().trim();
        String type      = typeCombo.getValue();
        String priceText = priceField.getText().trim();

        // Validation
        if (name.isEmpty()) {
            showError("Please enter item name");
            return;
        }
        if (type == null) {
            showError("Please select item type");
            return;
        }
        if (priceText.isEmpty()) {
            showError("Please enter starting price");
            return;
        }
        if (selectedImage == null) {
            showError("Please select an image");
            return;
        }

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

        Button submitBtn = (Button)event.getSource();
        submitBtn.setDisable(true);

        new Thread(() -> {
            try {
                // Chuyển đổi ảnh thành byte array
                byte[] imageBytes = FileUtils.toByteArray(selectedImage);
                if (imageBytes == null) {
                    Platform.runLater(() -> {
                        showError("Error reading image file");
                        submitBtn.setDisable(false);
                    });
                    return;
                }

                // Tạo item đúng loại và truyền ảnh
                ItemType itemType = ItemType.valueOf(type);
                Item item = switch (itemType) {
                    case POSTER -> new Poster(name, sellerId, imageBytes);
                    case FIGURE -> new Figure(name, sellerId, imageBytes);
                    case CARD   -> new Card(name, sellerId, imageBytes);
                };

                // Dùng ItemService để insert
                boolean itemOk = itemService.addItem(item);
                if (!itemOk) {
                    Platform.runLater(() -> {
                        showError("Failed to create item");
                        submitBtn.setDisable(false);
                    });
                    return;
                }

                // Tính thời gian: start = now, end = now + 2 ngày
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime   = startTime.plusDays(2);

                Auction auction = new Auction(
                        0, item.getId(),
                        startingPrice, startingPrice,
                        startTime, endTime,
                        AuctionStatus.OPEN
                );

                // Dùng AuctionService để insert
                boolean auctionOk = auctionService.createAuction(auction);
                if (!auctionOk) {
                    Platform.runLater(() -> {
                        showError("Failed to create auction");
                        submitBtn.setDisable(false);
                    });
                    return;
                }

                Platform.runLater(() -> {
                    nameField.clear();
                    typeCombo.setValue(null);
                    priceField.clear();
                    uploadLabel.setText("");
                    selectedImage = null;
                    showSuccess("Item listed successfully!");
                    submitBtn.setDisable(false);
                    loadMyItems();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error: " + e.getMessage());
                    submitBtn.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #A32D2D;");
        messageLabel.setText(message);
    }

    private void showSuccess(String message) {
        messageLabel.setStyle("-fx-text-fill: #3B6D11;");
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

