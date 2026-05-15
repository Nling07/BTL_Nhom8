package com.btl.n8.Controller;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.entity.Item;
import com.btl.n8.Service.AuctionService;
import com.btl.n8.Service.ItemService;
import com.btl.n8.dto.BidResponse;
import com.btl.n8.network.ClientSocket;
import com.btl.n8.network.ServerResponseListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * bidController — màn hình danh sách item/auction để đặt giá.
 *
 * Các cải tiến so với version cũ:
 *  1. FIX N+1 QUERY: dùng JOIN query 1 lần thay vì loop N query.
 *  2. FIX REAL-TIME PRICE: ItemRow dùng JavaFX Properties (SimpleStringProperty)
 *     nên TableView tự re-render ngay khi có bid mới, không cần reload trang.
 *  3. SOCKET LISTENER: implement ServerResponseListener, lắng nghe broadcast
 *     BID_UPDATE từ server và update đúng row trong bảng theo auctionId.
 */
public class bidController implements ServerResponseListener {

    // ── FXML bindings ──────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private TableView<ItemRow> itemTable;
    @FXML private TableColumn<ItemRow, String> colId;
    @FXML private TableColumn<ItemRow, String> colName;
    @FXML private TableColumn<ItemRow, String> colType;
    @FXML private TableColumn<ItemRow, String> colPrice;
    @FXML private TableColumn<ItemRow, String> colStatus;
    @FXML private TableColumn<ItemRow, Void>   colAction;

    // ── State ──────────────────────────────────────────────────────────────────
    private final ObservableList<ItemRow> allItems = FXCollections.observableArrayList();
    private ItemService    itemService;
    private AuctionService auctionService;
    private static final Gson gson = new Gson();

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        loadData();

        // Đăng ký lắng nghe broadcast từ server (BID_UPDATE)
        // ClientSocket đã có sẵn singleton, chỉ cần addListener
        ClientSocket.getInstance().addListener(this);
    }

    /**
     * Gọi khi màn hình bị đóng (hoặc Return về home).
     * Quan trọng: phải removeListener để tránh memory leak và
     * tránh update một controller đã chết.
     */
    private void cleanup() {
        ClientSocket.getInstance().removeListener(this);
    }

    // ── Column setup ───────────────────────────────────────────────────────────

    private void setupColumns() {
        // FIX: dùng lambda bind vào Property thay vì PropertyValueFactory
        // để TableView tự observe và re-render khi property thay đổi.
        colId    .setCellValueFactory(c -> c.getValue().idProperty());
        colName  .setCellValueFactory(c -> c.getValue().nameProperty());
        colType  .setCellValueFactory(c -> c.getValue().typeProperty());
        colPrice .setCellValueFactory(c -> c.getValue().priceProperty());
        colStatus.setCellValueFactory(c -> c.getValue().statusProperty());

        colAction.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("Bid");

            {
                btn.setOnAction(e -> {
                    ItemRow row = getTableRow().getItem();
                    if (row == null) return;
                    btn.setDisable(true);
                    openBidPopup(row, btn);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ItemRow row = getTableRow().getItem();
                    // Chỉ enable nút Bid nếu item có auction đang OPEN
                    boolean canBid = row != null
                            && row.getAuctionId() != -1
                            && "OPEN".equals(row.getStatus());
                    btn.setDisable(!canBid);
                    setGraphic(btn);
                }
            }
        });
    }

    // ── Data loading (FIX N+1 QUERY) ──────────────────────────────────────────

    /**
     * Load dữ liệu trong background thread.
     *
     * FIX N+1: Thay vì gọi getAllItems() rồi loop getAuctionByItemId() cho từng item
     * (= 1 + N query), ta dùng AuctionDAO.findAllWithItems() — một JOIN query duy nhất.
     * Xem AuctionDAOImpl.findAllWithItems() bên dưới để hiểu SQL.
     */
    private void loadData() {
        new Thread(() -> {
            try {
                Connection conn = DataConnection.getConnection();
                if (conn == null) throw new Exception("Database connection failed");

                itemService    = new ItemService(new ItemDAOImpl(conn));
                auctionService = new AuctionService(new AuctionDAOImpl(conn));

                // 1 query JOIN thay vì N+1
                List<ItemAuctionRow> joined = ((AuctionDAOImpl) auctionService
                        .getAuctionDAO())
                        .findAllWithItems();

                ObservableList<ItemRow> tempList = FXCollections.observableArrayList();
                for (ItemAuctionRow r : joined) {
                    tempList.add(new ItemRow(
                            r.itemId,
                            r.itemName,
                            r.itemType,
                            r.currentPrice != null
                                    ? String.format("%,.0f ₫", r.currentPrice)
                                    : "-",
                            r.status != null ? r.status : "NO AUCTION",
                            r.auctionId
                    ));
                }

                Platform.runLater(() -> {
                    allItems.setAll(tempList);
                    itemTable.setItems(allItems);
                });

            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load items: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    // ── Socket listener (REAL-TIME PRICE UPDATE) ───────────────────────────────

    /**
     * Được gọi bởi ClientSocket mỗi khi server broadcast một message.
     * Server broadcast BID_UPDATE sau mỗi lần đặt giá thành công (xem ClientHandler).
     *
     * FIX REAL-TIME: Tìm đúng ItemRow theo auctionId, gọi setPrice() / setStatus()
     * → vì các field là SimpleStringProperty, TableView tự re-render ngay lập tức
     * mà không cần reload toàn bộ bảng.
     */
    @Override
    public void onRespone(JsonObject response) {
        // Chỉ xử lý BID_UPDATE
        if (!response.has("action")) return;
        String action = response.get("action").getAsString();
        if (!"BID_UPDATE".equals(action)) return;

        BidResponse res = gson.fromJson(response, BidResponse.class);
        if (!res.isSuccess()) return;

        Platform.runLater(() -> {
            // Tìm row có đúng auctionId và update price tại chỗ
            for (ItemRow row : allItems) {
                if (row.getAuctionId() == res.getAuctionId()) {
                    // SimpleStringProperty.set() → TableView tự observe và re-render
                    row.setPrice(String.format("%,.0f ₫", res.getCurrentPrice()));
                    row.setStatus("OPEN"); // vẫn OPEN nếu bid thành công
                    break;
                }
            }
        });
    }

    // ── Bid popup ──────────────────────────────────────────────────────────────

    private void openBidPopup(ItemRow row, Button btn) {
        if (itemService == null) {
            showError("Data is still loading");
            btn.setDisable(false);
            return;
        }

        new Thread(() -> {
            try {
                Item item = itemService.getItemById(row.getId());
                if (item == null) {
                    Platform.runLater(() -> {
                        showError("Item not found");
                        btn.setDisable(false);
                    });
                    return;
                }

                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(
                                getClass().getResource("/fxml/bidDetails.fxml"));
                        Parent root = loader.load();

                        bidDetailController controller = loader.getController();
                        controller.initData(row.getAuctionId(), item);

                        Stage popup = new Stage();
                        popup.initStyle(StageStyle.UNDECORATED);
                        popup.setTitle("Bid - " + row.getName());
                        popup.setScene(new Scene(root));
                        popup.initModality(Modality.APPLICATION_MODAL);
                        popup.setResizable(false);
                        popup.setOnHidden(e -> btn.setDisable(false));
                        popup.showAndWait();

                    } catch (Exception ex) {
                        showError("Failed to open auction: " + ex.getMessage());
                        btn.setDisable(false);
                        ex.printStackTrace();
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Failed to load item: " + e.getMessage());
                    btn.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    // ── Search ─────────────────────────────────────────────────────────────────

    @FXML
    public void handleSearch(ActionEvent event) {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            itemTable.setItems(allItems);
            return;
        }

        ObservableList<ItemRow> filtered = FXCollections.observableArrayList();

        try {
            int id = Integer.parseInt(query);
            filtered.addAll(
                    allItems.stream()
                            .filter(r -> r.getId() == id)
                            .collect(Collectors.toList())
            );
        } catch (NumberFormatException e) {
            filtered.addAll(
                    allItems.stream()
                            .filter(r -> r.getName()
                                    .toLowerCase()
                                    .contains(query.toLowerCase()))
                            .collect(Collectors.toList())
            );
        }

        itemTable.setItems(filtered);
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    @FXML
    public void Return(ActionEvent event) throws Exception {
        cleanup(); // bỏ listener trước khi rời màn hình

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/home.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── Inner classes ──────────────────────────────────────────────────────────

    /**
     * ItemRow — model cho từng dòng trong TableView.
     *
     * FIX: Các field price và status dùng SimpleStringProperty thay vì final String.
     * TableView bind vào Property → khi gọi setPrice() / setStatus(), nó tự re-render
     * ngay lập tức mà không cần reload toàn bộ ObservableList.
     *
     * id, name, type vẫn là SimpleStringProperty (immutable về mặt logic) để đồng nhất.
     */
    public static class ItemRow {

        private final int id;
        private final int auctionId;

        private final SimpleStringProperty idProp;
        private final SimpleStringProperty name;
        private final SimpleStringProperty type;
        private final SimpleStringProperty price;   // mutable — thay đổi khi có bid mới
        private final SimpleStringProperty status;  // mutable — thay đổi khi đấu giá kết thúc

        public ItemRow(int id, String name, String type,
                       String price, String status, int auctionId) {
            this.id        = id;
            this.auctionId = auctionId;
            this.idProp    = new SimpleStringProperty(String.valueOf(id));
            this.name      = new SimpleStringProperty(name);
            this.type      = new SimpleStringProperty(type);
            this.price     = new SimpleStringProperty(price);
            this.status    = new SimpleStringProperty(status);
        }

        // Properties (TableView bind vào đây)
        public SimpleStringProperty idProperty()     { return idProp; }
        public SimpleStringProperty nameProperty()   { return name; }
        public SimpleStringProperty typeProperty()   { return type; }
        public SimpleStringProperty priceProperty()  { return price; }
        public SimpleStringProperty statusProperty() { return status; }

        // Getters
        public int    getId()        { return id; }
        public int    getAuctionId() { return auctionId; }
        public String getName()      { return name.get(); }
        public String getType()      { return type.get(); }
        public String getPrice()     { return price.get(); }
        public String getStatus()    { return status.get(); }

        // Setters cho real-time update (chỉ price và status cần thay đổi)
        public void setPrice(String newPrice)   { price.set(newPrice); }
        public void setStatus(String newStatus) { status.set(newStatus); }
    }

    /**
     * DTO nhẹ dùng nội bộ để nhận kết quả JOIN query.
     * Không cần serialize, chỉ dùng trong loadData().
     */
    public static class ItemAuctionRow {
        public int    itemId;
        public String itemName;
        public String itemType;
        public java.math.BigDecimal currentPrice;
        public String status;
        public int    auctionId;

        public ItemAuctionRow(int itemId, String itemName, String itemType,
                              java.math.BigDecimal currentPrice,
                              String status, int auctionId) {
            this.itemId       = itemId;
            this.itemName     = itemName;
            this.itemType     = itemType;
            this.currentPrice = currentPrice;
            this.status       = status;
            this.auctionId    = auctionId;
        }
    }
}