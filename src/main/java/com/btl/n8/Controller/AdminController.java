package com.btl.n8.Controller;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Item;
import com.btl.n8.Model.Entity.User;
import com.btl.n8.Model.Enums.AuctionStatus;
import com.btl.n8.Service.AdminService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class AdminController {

    // ── Sidebar nav buttons ───────────────────────────────────────────────────
    @FXML private Button navDashboard;
    @FXML private Button navUsers;
    @FXML private Button navAuctions;
    @FXML private Button navItems;

    // ── Content panels ────────────────────────────────────────────────────────
    @FXML private StackPane contentArea;
    @FXML private VBox panelDashboard;
    @FXML private VBox panelUsers;
    @FXML private VBox panelAuctions;
    @FXML private VBox panelItems;

    // ── Dashboard labels ──────────────────────────────────────────────────────
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalAuctions;
    @FXML private Label lblOpenAuctions;
    @FXML private Label lblTotalItems;
    @FXML private Label lblTotalSellers;
    @FXML private Label lblCancelledAuctions;
    @FXML private Label lblAdminAccount;

    // ── User table ────────────────────────────────────────────────────────────
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUserId;
    @FXML private TableColumn<User, String> colUserAccount;
    @FXML private TableColumn<User, String> colUserRole;
    @FXML private TableColumn<User, String> colUserBalance;
    @FXML private TableColumn<User, String> colUserActions;

    // ── Auction table ─────────────────────────────────────────────────────────
    @FXML private TableView<Auction> tableAuctions;
    @FXML private TableColumn<Auction, String> colAuctionId;
    @FXML private TableColumn<Auction, String> colAuctionItemId;
    @FXML private TableColumn<Auction, String> colAuctionStartPrice;
    @FXML private TableColumn<Auction, String> colAuctionCurrentPrice;
    @FXML private TableColumn<Auction, String> colAuctionStatus;
    @FXML private TableColumn<Auction, String> colAuctionEndTime;
    @FXML private TableColumn<Auction, String> colAuctionActions;

    // ── Item table ────────────────────────────────────────────────────────────
    @FXML private TableView<Item> tableItems;
    @FXML private TableColumn<Item, String> colItemId;
    @FXML private TableColumn<Item, String> colItemName;
    @FXML private TableColumn<Item, String> colItemType;
    @FXML private TableColumn<Item, String> colItemSeller;
    @FXML private TableColumn<Item, String> colItemActions;

    private AdminService adminService;

    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        try {
            Connection conn = DataConnection.getConnection();
            adminService = new AdminService(
                    new UserDAOImpl(conn),
                    new AuctionDAOImpl(conn),
                    new ItemDAOImpl(conn)
            );
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối DB: " + e.getMessage());
            return;
        }

        // Hiển thị tên admin
        User admin = SessionManager.getInstance().getCurrentUser();
        if (admin != null && lblAdminAccount != null) {
            lblAdminAccount.setText(admin.getAccount());
        }

        setupUserTable();
        setupAuctionTable();
        setupItemTable();

        showPanel("dashboard");
        loadDashboard();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    public void onNavDashboard(ActionEvent e) {
        showPanel("dashboard");
        loadDashboard();
    }

    @FXML
    public void onNavUsers(ActionEvent e) {
        showPanel("users");
        loadUsers();
    }

    @FXML
    public void onNavAuctions(ActionEvent e) {
        showPanel("auctions");
        loadAuctions();
    }

    @FXML
    public void onNavItems(ActionEvent e) {
        showPanel("items");
        loadItems();
    }

    @FXML
    public void onLogout(ActionEvent event) throws Exception {
        SessionManager.getInstance().logout();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    private void loadDashboard() {
        new Thread(() -> {
            int users     = adminService.getTotalUsers();
            int auctions  = adminService.getTotalAuctions();
            int open      = adminService.getOpenAuctionCount();
            int items     = adminService.getTotalItems();
            int sellers   = adminService.getTotalSellers();
            int cancelled = adminService.getCancelledAuctionCount();
            Platform.runLater(() -> {
                lblTotalUsers.setText(String.valueOf(users));
                lblTotalAuctions.setText(String.valueOf(auctions));
                lblOpenAuctions.setText(String.valueOf(open));
                lblTotalItems.setText(String.valueOf(items));
                lblTotalSellers.setText(String.valueOf(sellers));
                lblCancelledAuctions.setText(String.valueOf(cancelled));
            });
        }).start();
    }

    // ── User table setup & load ───────────────────────────────────────────────

    private void setupUserTable() {
        colUserId.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colUserAccount.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getAccount()));
        colUserRole.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getRole().name()));
        colUserBalance.setCellValueFactory(d -> {
            var balance = d.getValue().getBalance();
            return new SimpleStringProperty(balance != null ? balance.toPlainString() : "0");
        });

        // Action buttons column
        colUserActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnUpgrade = new Button("↑ Seller");
            private final Button btnDemote  = new Button("↓ Bidder");
            private final Button btnDelete  = new Button("Xóa");

            {
                btnUpgrade.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (confirm("Nâng cấp " + u.getAccount() + " lên Seller?")) {
                        adminService.upgradeUserToSeller(u.getId());
                        loadUsers();
                    }
                });
                btnDemote.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (confirm("Hạ cấp " + u.getAccount() + " về Bidder?")) {
                        adminService.demoteSellerToBidder(u.getId());
                        loadUsers();
                    }
                });
                btnDelete.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (confirm("Xóa tài khoản " + u.getAccount() + "? Hành động này không thể hoàn tác!")) {
                        adminService.deleteUserById(u.getId());
                        loadUsers();
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(4,
                            btnUpgrade, btnDemote, btnDelete);
                    setGraphic(box);
                }
            }
        });
    }

    private void loadUsers() {
        new Thread(() -> {
            List<User> users = adminService.getAllUsers();
            Platform.runLater(() ->
                    tableUsers.setItems(FXCollections.observableArrayList(users)));
        }).start();
    }

    // ── Auction table setup & load ────────────────────────────────────────────

    private void setupAuctionTable() {
        colAuctionId.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colAuctionItemId.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getItemId())));
        colAuctionStartPrice.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStartingPrice().toPlainString()));
        colAuctionCurrentPrice.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCurrentPrice().toPlainString()));
        colAuctionStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus().name()));
        colAuctionEndTime.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEndTime().toString()));

        colAuctionActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnClose  = new Button("Đóng");
            private final Button btnCancel = new Button("Hủy");
            private final Button btnDelete = new Button("Xóa");

            {
                btnClose.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    if (a.getStatus() != AuctionStatus.OPEN) {
                        showAlert(Alert.AlertType.WARNING, "Phiên này không đang OPEN.");
                        return;
                    }
                    if (confirm("Đóng sớm phiên đấu giá #" + a.getId() + "?")) {
                        adminService.closeAuction(a.getId());
                        loadAuctions();
                    }
                });
                btnCancel.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    if (confirm("Hủy phiên đấu giá #" + a.getId() + " (CANCELLED)?")) {
                        adminService.cancelAuction(a.getId());
                        loadAuctions();
                    }
                });
                btnDelete.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    if (confirm("Xóa vĩnh viễn phiên #" + a.getId() + "?")) {
                        adminService.deleteAuctionById(a.getId());
                        loadAuctions();
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Auction a = getTableView().getItems().get(getIndex());
                    javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(4);
                    if (a.getStatus() == AuctionStatus.OPEN) box.getChildren().addAll(btnClose, btnCancel);
                    box.getChildren().add(btnDelete);
                    setGraphic(box);
                }
            }
        });
    }

    private void loadAuctions() {
        new Thread(() -> {
            List<Auction> auctions = adminService.getAllAuctions();
            Platform.runLater(() ->
                    tableAuctions.setItems(FXCollections.observableArrayList(auctions)));
        }).start();
    }

    // ── Item table setup & load ───────────────────────────────────────────────

    private void setupItemTable() {
        colItemId.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colItemName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getName()));
        colItemType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getType().name()));
        colItemSeller.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getSellerId())));

        colItemActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Xóa");

            {
                btnDelete.setOnAction(e -> {
                    Item item = getTableView().getItems().get(getIndex());
                    if (confirm("Xóa vật phẩm \"" + item.getName() + "\"?")) {
                        adminService.deleteItemById(item.getId());
                        loadItems();
                    }
                });
            }

            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });
    }

    private void loadItems() {
        new Thread(() -> {
            List<Item> items = adminService.getAllItems();
            Platform.runLater(() ->
                    tableItems.setItems(FXCollections.observableArrayList(items)));
        }).start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showPanel(String name) {
        panelDashboard.setVisible(false); panelDashboard.setManaged(false);
        panelUsers.setVisible(false);     panelUsers.setManaged(false);
        panelAuctions.setVisible(false);  panelAuctions.setManaged(false);
        panelItems.setVisible(false);     panelItems.setManaged(false);

        switch (name) {
            case "dashboard" -> { panelDashboard.setVisible(true); panelDashboard.setManaged(true); }
            case "users"     -> { panelUsers.setVisible(true);     panelUsers.setManaged(true);     }
            case "auctions"  -> { panelAuctions.setVisible(true);  panelAuctions.setManaged(true);  }
            case "items"     -> { panelItems.setVisible(true);     panelItems.setManaged(true);     }
        }
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message,
                ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showAlert(Alert.AlertType type, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type, message, ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }
}