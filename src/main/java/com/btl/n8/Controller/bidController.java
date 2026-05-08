package com.btl.n8.Controller;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.*;

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
                    openBidPopup(row);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void openBidPopup(ItemRow row) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/bidDetails.fxml"));
            Parent root = loader.load();

            // Lấy item thực từ DB
            try (Connection conn = DataConnection.getConnection()) {
                ItemDAO itemDAO = new ItemDAOImpl(conn);
                Item item = itemDAO.findById(row.getId());

                if (item == null) {
                    showError("Item not found");
                    return;
                }

                // Truyền data vào controller popup
                bidDetailController controller = loader.getController();
                controller.initData(row.getAuctionId(), item);

                // Tạo popup stage đè lên màn hình bid
                Stage popup = new Stage();
                popup.setTitle("Bid - " + row.getName());
                popup.setScene(new Scene(root));
                popup.initModality(Modality.APPLICATION_MODAL);
                popup.setResizable(false);
                popup.show();
            }
        } catch (Exception ex) {
            showError("Failed to open auction: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadData() {
        new Thread(() -> {
            try (Connection conn = DataConnection.getConnection()) {
                if (conn == null) {
                    throw new Exception("Failed to connect to database");
                }
                ItemDAO itemDAO       = new ItemDAOImpl(conn);
                AuctionDAO auctionDAO = new AuctionDAOImpl(conn);

                List<Item> items = itemDAO.findAll();

                for (Item item : items) {
                    Auction auction = auctionDAO.findByItemId(item.getId());
                    String price    = auction != null ? auction.getCurrentPrice().toPlainString() + " ₫" : "-";
                    String status   = auction != null ? auction.getStatus().name() : "NO AUCTION";
                    int auctionId   = auction != null ? auction.getId() : -1;

                    allItems.add(new ItemRow(
                            item.getId(),
                            item.getName(),
                            item.getType().name(),
                            price,
                            status,
                            auctionId
                    ));
                }

                Platform.runLater(() -> itemTable.setItems(allItems));

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Failed to load items: " + e.getMessage());
                });
            }
        }).start();
    }


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

    @FXML
    public void Return(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/home.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static class ItemRow {
        private final int id;
        private final String name;
        private final String type;
        private final String price;
        private final String status;
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