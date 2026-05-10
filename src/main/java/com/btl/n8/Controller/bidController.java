package com.btl.n8.Controller;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.*;
import com.btl.n8.Service.AuctionService;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

public class bidController {

    @FXML private TextField searchField;
    @FXML private TableView<ItemRow> itemTable;
    @FXML private TableColumn<ItemRow, Integer> colId;
    @FXML private TableColumn<ItemRow, String>  colName;
    @FXML private TableColumn<ItemRow, String>  colType;
    @FXML private TableColumn<ItemRow, String>  colPrice;
    @FXML private TableColumn<ItemRow, String>  colStatus;
    @FXML private TableColumn<ItemRow, Void>    colAction;

    private ObservableList<ItemRow> allItems = FXCollections.observableArrayList();
    private ItemService itemService;
    private AuctionService auctionService;

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Bid");
            {
                btn.setOnAction(e -> {
                    ItemRow row = getTableView().getItems().get(getIndex());
                    btn.setDisable(true);
                    openBidPopup(row, btn);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void openBidPopup(ItemRow row, Button btn) {
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
                        popup.setTitle("Bid - " + row.getName());
                        popup.setScene(new Scene(root));
                        popup.initModality(Modality.APPLICATION_MODAL);
                        popup.setResizable(false);
                        popup.setOnHidden(e -> btn.setDisable(false));
                        popup.show();

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

    private void loadData() {
        new Thread(() -> {
            try {
                Connection conn = DataConnection.getConnection();
                if (conn == null) throw new Exception("Database connection failed");

                itemService    = new ItemService(new ItemDAOImpl(conn));
                auctionService = new AuctionService(new AuctionDAOImpl(conn));

                List<Item> items = itemService.getAllItems();
                for (Item item : items) {
                    Auction auction = auctionService.getAuctionByItemId(item.getId());
                    String price    = auction != null ? auction.getCurrentPrice().toPlainString() + " ₫" : "-";
                    String status   = auction != null ? auction.getStatus().name() : "NO AUCTION";
                    int auctionId   = auction != null ? auction.getId() : -1;

                    allItems.add(new ItemRow(
                            item.getId(), item.getName(), item.getType().name(),
                            price, status, auctionId
                    ));
                }

                Platform.runLater(() -> itemTable.setItems(allItems));

            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load items: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        String query = searchField.getText().trim();
        if (query.isEmpty()) { itemTable.setItems(allItems); return; }

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

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void Return(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/home.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static class ItemRow {
        private final int id;
        private final String name, type, price, status;
        private final int auctionId;

        public ItemRow(int id, String name, String type, String price, String status, int auctionId) {
            this.id = id; this.name = name; this.type = type;
            this.price = price; this.status = status; this.auctionId = auctionId;
        }

        public int    getId()        { return id; }
        public String getName()      { return name; }
        public String getType()      { return type; }
        public String getPrice()     { return price; }
        public String getStatus()    { return status; }
        public int    getAuctionId() { return auctionId; }
    }
}