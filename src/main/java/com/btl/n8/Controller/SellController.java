package com.btl.n8.Controller;

import com.btl.n8.Connection.AuctionDAOImpl;
import com.btl.n8.Connection.DataConnection;
import com.btl.n8.Connection.ItemDAOImpl;
import com.btl.n8.DTO.AddItemRequest;
import com.btl.n8.DTO.AddItemResponse;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Item;
import com.btl.n8.Model.Entity.User;
import com.btl.n8.Model.Enums.ItemType;
import com.btl.n8.Model.Enums.Role;
import com.btl.n8.Network.ClientSocket;
import com.btl.n8.Network.ServerResponseListener;
import com.btl.n8.Service.AuctionService;
import com.btl.n8.Service.ItemService;
import com.btl.n8.Util.FileUtils;
import com.btl.n8.Util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

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
import java.util.Base64;
import java.util.List;

/**
 * SellController — tuân thủ kiến trúc DTO:
 * Controller chỉ tương tác với Service qua DTO (AddItemRequest / AddItemResponse).
 * Luồng: Controller → AddItemRequest → ClientSocket → Server → AddItemResponse → Controller.
 *
 * Bảng "my items" vẫn load trực tiếp từ DB (read-only, không cần qua server).
 */
public class SellController implements ServerResponseListener {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField priceField;
    @FXML private Spinner<Integer> hoursSpinner;
    @FXML private Spinner<Integer> minutesSpinner;
    @FXML private Label uploadLabel;
    @FXML private Label messageLabel;

    @FXML private TableView<SellRow> myItemTable;
    @FXML private TableColumn<SellRow, Integer> colId;
    @FXML private TableColumn<SellRow, String>  colName;
    @FXML private TableColumn<SellRow, String>  colType;
    @FXML private TableColumn<SellRow, String>  colStartPrice;
    @FXML private TableColumn<SellRow, String>  colCurrentPrice;
    @FXML private TableColumn<SellRow, String>  colStatus;

    // ── State ─────────────────────────────────────────────────────────────────
    private File selectedImage;
    private final ObservableList<SellRow> myItems = FXCollections.observableArrayList();
    private int sellerId;

    // Các service chỉ dùng để đọc bảng "my items" (không dùng để ghi)
    private ItemService    itemService;
    private AuctionService auctionService;

    // Submit button — giữ ref để re-enable sau khi nhận response
    private Button pendingSubmitBtn;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();

        // Chặn người không phải SELLER
        if (user == null || user.getRole() != Role.SELLER) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Không có quyền");
                alert.setHeaderText(null);
                alert.setContentText("Chỉ Seller mới có thể đăng bán sản phẩm.\nHãy nâng cấp tài khoản tại trang Home.");
                alert.showAndWait();
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/fxml/home.fxml"));
                    Stage stage = (Stage) nameField.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            return;
        }

        sellerId = user.getId();

        // Đăng ký lắng nghe response từ server
        ClientSocket.getInstance().addListener(this);

        // Khởi tạo ComboBox
        typeCombo.setItems(FXCollections.observableArrayList("POSTER", "FIGURE", "CARD"));

        // Spinner giờ: 0–48, mặc định 1
        hoursSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 48, 1));
        hoursSpinner.setEditable(true);

        // Spinner phút: 0–59, mặc định 0
        minutesSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        minutesSpinner.setEditable(true);

        // Cài cột bảng
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        myItemTable.setItems(myItems);

        // Load bảng từ DB (chỉ đọc)
        loadTableFromDB();
    }

    /** Gọi khi rời màn hình — hủy đăng ký listener để tránh memory leak. */
    private void cleanup() {
        ClientSocket.getInstance().removeListener(this);
    }

    // ── ServerResponseListener ────────────────────────────────────────────────

    /**
     * Nhận ADD_ITEM_SUCCESS từ server.
     * Controller KHÔNG tự tạo Item/Auction — chỉ phản ứng với response.
     */
    @Override
    public void onRespone(JsonObject response) {
        if (!response.has("action")) return;
        if (!"ADD_ITEM_SUCCESS".equals(response.get("action").getAsString())) return;

        AddItemResponse res = gson.fromJson(response, AddItemResponse.class);

        Platform.runLater(() -> {
            // Re-enable nút submit
            if (pendingSubmitBtn != null) {
                pendingSubmitBtn.setDisable(false);
                pendingSubmitBtn = null;
            }

            if (res.isSuccess()) {
                // Reset form
                nameField.clear();
                typeCombo.setValue(null);
                priceField.clear();
                uploadLabel.setText("");
                selectedImage = null;
                showSuccess("Item listed successfully! (ItemID=" + res.getItemId()
                        + ", AuctionID=" + res.getAuctionId() + ")");
                // Reload bảng để hiện item mới
                loadTableFromDB();
            } else {
                showError("Server từ chối: " + res.getMessage());
            }
        });
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML
    public void handleUpload(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select product image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        selectedImage = fc.showOpenDialog(stage);

        if (selectedImage != null) {
            uploadLabel.setText("✓ " + selectedImage.getName());
        }
    }

    /**
     * handleSell — validate → build AddItemRequest → gửi qua ClientSocket.
     * Không tự gọi ItemService.addItem() hay AuctionService.createAuction().
     * Server xử lý và trả về AddItemResponse, Controller phản ứng trong onRespone().
     */
    @FXML
    public void handleSell(ActionEvent event) {
        messageLabel.setText("");
        messageLabel.setStyle("");

        // ── Validate input ───────────────────────────────────────────────────
        String name      = nameField.getText().trim();
        String typeStr   = typeCombo.getValue();
        String priceText = priceField.getText().trim();

        if (name.isEmpty())        { showError("Please enter item name"); return; }
        if (typeStr == null)       { showError("Please select item type"); return; }
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

        ItemType itemType;
        try {
            itemType = ItemType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            showError("Invalid item type: " + typeStr);
            return;
        }

        // Validate duration
        hoursSpinner.commitValue();
        minutesSpinner.commitValue();
        int hours   = hoursSpinner.getValue();
        int minutes = minutesSpinner.getValue();
        int totalMinutes = hours * 60 + minutes;
        if (totalMinutes < 1) {
            showError("Duration must be at least 1 minute");
            return;
        }
        if (totalMinutes > 48 * 60) {
            showError("Duration cannot exceed 48 hours");
            return;
        }

        // Disable nút để tránh double-submit
        Button submitBtn = (Button) event.getSource();
        submitBtn.setDisable(true);
        pendingSubmitBtn = submitBtn;

        // ── Build và gửi request trên background thread ──────────────────────
        final int fHours = hours, fMinutes = minutes;

        new Thread(() -> {
            try {
                // Đọc ảnh thành byte[], rồi encode Base64 để gửi qua socket (JSON safe)
                byte[] imageBytes = FileUtils.toByteArray(selectedImage);
                if (imageBytes == null) {
                    Platform.runLater(() -> {
                        showError("Error reading image file");
                        submitBtn.setDisable(false);
                        pendingSubmitBtn = null;
                    });
                    return;
                }
                String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime   = startTime.plusHours(fHours).plusMinutes(fMinutes);

                String sessionId = SessionManager.getInstance().getSessionId();

                // ← Đây là điểm duy nhất Controller tương tác với "service" — qua DTO
                AddItemRequest req = new AddItemRequest(
                        name, sessionId, itemType,
                        startingPrice, imageBase64,
                        startTime, endTime,
                        sellerId
                );

                ClientSocket.getInstance().sendMessage(req);
                // Response sẽ đến qua onRespone() → Platform.runLater

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error preparing request: " + e.getMessage());
                    submitBtn.setDisable(false);
                    pendingSubmitBtn = null;
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void Return(ActionEvent event) throws Exception {
        cleanup();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/home.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    // ── Table loading (đọc DB trực tiếp — chỉ cho UI hiển thị) ───────────────

    /** Khởi tạo service đọc DB (lần đầu) và reload bảng. */
    private void loadTableFromDB() {
        new Thread(() -> {
            try {
                Connection conn = DataConnection.getConnection();
                if (conn == null) throw new Exception("Database connection failed");

                // Lazy-init service chỉ dùng để đọc
                if (itemService == null)    itemService    = new ItemService(new ItemDAOImpl(conn));
                if (auctionService == null) auctionService = new AuctionService(new AuctionDAOImpl(conn));

                List<Item> items = itemService.getItemsBySeller(sellerId);
                List<SellRow> rows = buildRows(items);

                Platform.runLater(() -> {
                    myItems.clear();
                    myItems.addAll(rows);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load data: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #ff6b6b;");
        messageLabel.setText(message);
    }

    private void showSuccess(String message) {
        messageLabel.setStyle("-fx-text-fill: #00ff88;");
        messageLabel.setText(message);
    }

    private String fmt(BigDecimal n) {
        return String.format("%,.0f ₫", n);
    }

    // ── Inner class ───────────────────────────────────────────────────────────

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