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

        // Cột Bid button
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Bid");
            {
                btn.setOnAction(e -> {
                    ItemRow row = getTableView().getItems().get(getIndex());
                    System.out.println("Bid on item: " + row.getId());
                    // TODO: mở màn hình đặt giá, truyền row.getAuctionId()
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void loadData() {
        try {
            Connection conn = DataConnection.getConnection();
            ItemDAO itemDAO = new ItemDAOImpl(conn);
            AuctionDAO auctionDAO = new AuctionDAOImpl(conn);

            List<Item> items = itemDAO.findAll();

            for (Item item : items) {
                Auction auction = auctionDAO.findByItemId(item.getId());
                String price  = auction != null ? auction.getCurrentPrice().toPlainString() + " ₫" : "-";
                String status = auction != null ? auction.getStatus().name() : "NO AUCTION";
                int auctionId = auction != null ? auction.getId() : -1;

                allItems.add(new ItemRow(
                        item.getId(),
                        item.getName(),
                        item.getType().name(),
                        price,
                        status,
                        auctionId
                ));
            }

            itemTable.setItems(allItems);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            itemTable.setItems(allItems);
            return;
        }

        // Tìm theo ID nếu là số, theo tên nếu là chữ
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

    // Inner class để bind vào TableView
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

        public int getId()        { return id; }
        public String getName()   { return name; }
        public String getType()   { return type; }
        public String getPrice()  { return price; }
        public String getStatus() { return status; }
        public int getAuctionId() { return auctionId; }
    }
}