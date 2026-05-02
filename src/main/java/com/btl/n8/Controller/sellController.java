package com.btl.n8.Controller;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.*;

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

    // TODO: thay bằng ID seller đang đăng nhập, truyền từ loginController
    private int sellerId = 1;

    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;

    @FXML
    public void initialize() {
        // Setup ComboBox
        typeCombo.setItems(FXCollections.observableArrayList("POSTER", "FIGURE", "CARD"));

        // Setup columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        myItemTable.setItems(myItems);

        // Kết nối DB
        try {
            Connection conn = DataConnection.getConnection();
            itemDAO    = new ItemDAOImpl(conn);
            auctionDAO = new AuctionDAOImpl(conn);
            loadMyItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMyItems() {
        myItems.clear();
        List<Item> items = itemDAO.findBySeller(sellerId);

        for (Item item : items) {
            Auction auction = auctionDAO.findByItemId(item.getId());
            String startPrice   = auction != null ? fmt(auction.getStartingPrice()) : "-";
            String currentPrice = auction != null ? fmt(auction.getCurrentPrice())  : "-";
            String status       = auction != null ? auction.getStatus().name()       : "NO AUCTION";

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
            uploadLabel.setText("✓ " + selectedImage.getName());
        }
    }

    @FXML
    public void handleSell(ActionEvent event) {
        messageLabel.setText("");

        String name     = nameField.getText().trim();
        String type     = typeCombo.getValue();
        String priceText = priceField.getText().trim();

        // Validate
        if (name.isEmpty() || type == null || priceText.isEmpty()) {
            messageLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        BigDecimal startingPrice;
        try {
            startingPrice = new BigDecimal(priceText);
            if (startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                messageLabel.setText("Giá phải lớn hơn 0!");
                return;
            }
        } catch (NumberFormatException e) {
            messageLabel.setText("Giá không hợp lệ!");
            return;
        }

        try {
            // Insert item
            ItemType itemType = ItemType.valueOf(type);
            Item item = switch (itemType) {
                case POSTER -> new Poster(0, name, sellerId);
                case FIGURE -> new Figure(0, name, sellerId);
                case CARD   -> new Card(0, name, sellerId);
            };

            boolean itemOk = itemDAO.insert(item);
            if (!itemOk) {
                messageLabel.setText("Lỗi khi tạo item!");
                return;
            }

            // Tự động tính thời gian: start = now, end = now + 2 ngày
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime   = startTime.plusDays(2);

            // Insert auction
            Auction auction = new Auction(
                    0, item.getId(),
                    startingPrice, startingPrice,
                    startTime, endTime,
                    AuctionStatus.OPEN
            );

            boolean auctionOk = auctionDAO.insert(auction);
            if (!auctionOk) {
                messageLabel.setText("Lỗi khi tạo auction!");
                return;
            }

            // Reset form
            nameField.clear();
            typeCombo.setValue(null);
            priceField.clear();
            uploadLabel.setText("");
            selectedImage = null;
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("✓ Đăng bán thành công!");

            // Reload bảng
            loadMyItems();

        } catch (Exception e) {
            messageLabel.setText("Lỗi hệ thống!");
            e.printStackTrace();
        }
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

    // Inner class bind TableView
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