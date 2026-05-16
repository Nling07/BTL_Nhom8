package com.btl.n8.Controller;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.Entity.Item;
import com.btl.n8.Service.AuctionService;
import com.btl.n8.Service.ItemService;
import com.btl.n8.DTO.BidResponse;
import com.btl.n8.Network.ClientSocket;
import com.btl.n8.Network.ServerResponseListener;
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

public class BidController implements ServerResponseListener {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private TableView<ItemRow> itemTable;
    @FXML private TableColumn<ItemRow, String> colId;
    @FXML private TableColumn<ItemRow, String> colName;
    @FXML private TableColumn<ItemRow, String> colType;
    @FXML private TableColumn<ItemRow, String> colPrice;
    @FXML private TableColumn<ItemRow, String> colStatus;
    @FXML private TableColumn<ItemRow, Void>   colAction;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ObservableList<ItemRow> allItems = FXCollections.observableArrayList();
    private ItemService       itemService;
    private AuctionService    auctionService;
    private AuctionDAOImpl    auctionDAOImpl;   // FIX: giữ ref để gọi closeExpiredAuctions
    private static final Gson gson = new Gson();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
        ClientSocket.getInstance().addListener(this);
    }

    private void cleanup() {
        ClientSocket.getInstance().removeListener(this);
    }

    // ── Column setup ──────────────────────────────────────────────────────────

    private void setupColumns() {
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
                    boolean canBid = row != null
                            && row.getAuctionId() != -1
                            && "OPEN".equals(row.getStatus());
                    btn.setDisable(!canBid);
                    setGraphic(btn);
                }
            }
        });
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    /**
     * FIX BUG 1: Gọi closeExpiredAuctions() TRƯỚC khi query danh sách.
     * DB sẽ tự update status CLOSED cho auction hết giờ → bảng hiển thị đúng.
     */
    private void loadData() {
        new Thread(() -> {
            try {
                Connection conn = DataConnection.getConnection();
                if (conn == null) throw new Exception("Database connection failed");

                itemService    = new ItemService(new ItemDAOImpl(conn));
                auctionDAOImpl = new AuctionDAOImpl(conn);
                auctionService = new AuctionService(auctionDAOImpl);

                // FIX BUG 1: đóng auction hết giờ trước khi load
                int closed = auctionDAOImpl.closeExpiredAuctions();
                if (closed > 0) {
                    System.out.println("Auto-closed " + closed + " expired auction(s).");
                }

                List<ItemAuctionRow> joined = auctionDAOImpl.findAllWithItems();

                ObservableList<ItemRow> tempList = FXCollections.observableArrayList();
                for (ItemAuctionRow r : joined) {
                    tempList.add(new ItemRow(
                            r.itemId, r.itemName, r.itemType,
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

    // ── Socket listener ───────────────────────────────────────────────────────

    /**
     * FIX BUG 3: Nhận BID_UPDATE → cập nhật ngay cả giá lẫn status trong bảng.
     * Không cần reload toàn bộ danh sách.
     */
    @Override
    public void onRespone(JsonObject response) {
        if (!response.has("action")) return;
        String action = response.get("action").getAsString();
        if (!"BID_UPDATE".equals(action)) return;

        BidResponse res = gson.fromJson(response, BidResponse.class);
        if (!res.isSuccess()) return;

        Platform.runLater(() -> {
            for (ItemRow row : allItems) {
                if (row.getAuctionId() == res.getAuctionId()) {
                    row.setPrice(String.format("%,.0f ₫", res.getCurrentPrice()));
                    row.setStatus("OPEN");
                    // Ép TableView refresh row bằng cách refresh toàn bộ
                    itemTable.refresh();
                    break;
                }
            }
        });
    }

    // ── Bid popup ─────────────────────────────────────────────────────────────

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

                        BidDetailController controller = loader.getController();

                        Stage popup = new Stage();
                        popup.initStyle(StageStyle.UNDECORATED);
                        popup.setTitle("Bid - " + row.getName());
                        popup.setScene(new Scene(root));
                        popup.initModality(Modality.APPLICATION_MODAL);
                        popup.setResizable(false);

                        popup.setOnHidden(e -> {
                            controller.cleanup();
                            btn.setDisable(false);
                            // FIX BUG 3: reload nhẹ sau khi đóng popup
                            // để bảng cập nhật giá + status mới nhất
                            refreshTableFromDB();
                        });

                        popup.show();
                        controller.initData(row.getAuctionId(), item);

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

    /**
     * FIX BUG 3: Reload nhẹ — chỉ update price + status cho từng row
     * mà không rebuild toàn bộ list (tránh nhấp nháy).
     */
    private void refreshTableFromDB() {
        if (auctionDAOImpl == null) return;
        new Thread(() -> {
            // đóng auction hết giờ trước
            auctionDAOImpl.closeExpiredAuctions();

            List<ItemAuctionRow> joined = auctionDAOImpl.findAllWithItems();
            Platform.runLater(() -> {
                for (ItemAuctionRow r : joined) {
                    for (ItemRow row : allItems) {
                        if (row.getId() == r.itemId) {
                            row.setPrice(r.currentPrice != null
                                    ? String.format("%,.0f ₫", r.currentPrice)
                                    : "-");
                            row.setStatus(r.status != null ? r.status : "NO AUCTION");
                            break;
                        }
                    }
                }
                itemTable.refresh();
            });
        }).start();
    }

    // ── Search ────────────────────────────────────────────────────────────────

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
            filtered.addAll(allItems.stream()
                    .filter(r -> r.getId() == id)
                    .collect(Collectors.toList()));
        } catch (NumberFormatException e) {
            filtered.addAll(allItems.stream()
                    .filter(r -> r.getName().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList()));
        }
        itemTable.setItems(filtered);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    public void Return(ActionEvent event) throws Exception {
        cleanup();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/home.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    public static class ItemRow {
        private final int id;
        private final int auctionId;
        private final SimpleStringProperty idProp;
        private final SimpleStringProperty name;
        private final SimpleStringProperty type;
        private final SimpleStringProperty price;
        private final SimpleStringProperty status;

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

        public SimpleStringProperty idProperty()     { return idProp; }
        public SimpleStringProperty nameProperty()   { return name; }
        public SimpleStringProperty typeProperty()   { return type; }
        public SimpleStringProperty priceProperty()  { return price; }
        public SimpleStringProperty statusProperty() { return status; }

        public int    getId()        { return id; }
        public int    getAuctionId() { return auctionId; }
        public String getName()      { return name.get(); }
        public String getType()      { return type.get(); }
        public String getPrice()     { return price.get(); }
        public String getStatus()    { return status.get(); }

        public void setPrice(String v)  { price.set(v); }
        public void setStatus(String v) { status.set(v); }
    }

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
