package com.btl.n8.Controller;

import com.btl.n8.Connection.DataConnection;
import com.btl.n8.Connection.UserDAO;
import com.btl.n8.Connection.UserDAOImpl;
import com.btl.n8.Model.Entity.Bidder;
import com.btl.n8.Model.Entity.User;
import com.btl.n8.Model.Enums.Role;

import com.btl.n8.Network.ClientSocket;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Optional;

public class HomeController {

    @FXML private Label balanceLabel;
    @FXML private Button becomeSellerBtn;

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            System.out.println("Warning: User not logged in");
            return;
        }
        refreshBalance();
        updateBecomeSellerVisibility();
    }

    private void refreshBalance() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (balanceLabel == null || user == null) return;
        BigDecimal bal = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        balanceLabel.setText(String.format("%,.0f", bal));
    }

    private void updateBecomeSellerVisibility() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (becomeSellerBtn != null) {
            // Chỉ hiện nút nếu user là BIDDER
            becomeSellerBtn.setVisible(user != null && user.getRole() == Role.BIDDER);
        }
    }

    @FXML
    public void deposit(ActionEvent event) {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        // Cả BIDDER và SELLER đều được nạp tiền
        if (user.getRole() != Role.BIDDER && user.getRole() != Role.SELLER) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Admin không thể nạp tiền.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp tiền");
        dialog.setHeaderText("Nhập số tiền muốn nạp:");
        dialog.setContentText("Số tiền:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                BigDecimal amount = new BigDecimal(input.trim().replace(",", ""));
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền phải lớn hơn 0.");
                    return;
                }

                BigDecimal current = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
                BigDecimal newBalance = current.add(amount);
                user.setBalance(newBalance);

                // Lưu vào DB
                Connection conn = DataConnection.getConnection();
                if (conn != null) {
                    new UserDAOImpl(conn).update(user);
                }

                refreshBalance();
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        String.format("Nạp thành công %,.0f ₫\nSố dư hiện tại: %,.0f ₫", amount, newBalance));

            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền không hợp lệ. Vui lòng nhập số.");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
            }
        });
    }

    @FXML
    public void becomeSeller(ActionEvent event) {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        if (user.getRole() != Role.BIDDER) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Bạn đã là Seller hoặc không đủ điều kiện.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText("Nâng cấp thành Seller?");
        confirm.setContentText("Bạn có muốn chuyển tài khoản từ Bidder sang Seller không?\n(Không thể hoàn tác)");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Connection conn = DataConnection.getConnection();
                if (conn == null) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối database.");
                    return;
                }

                UserDAO userDAO = new UserDAOImpl(conn);
                boolean success = userDAO.upgradeToSeller(user.getId());

                if (success) {
                    // Cập nhật session với user mới từ DB
                    User updatedUser = userDAO.findById(user.getId());
                    SessionManager.getInstance().setCurrentUser(updatedUser);

                    updateBecomeSellerVisibility();
                    showAlert(Alert.AlertType.INFORMATION, "Thành công",
                            "Tài khoản đã được nâng cấp thành Seller!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể nâng cấp tài khoản. Vui lòng thử lại.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
            }
        }
    }

    public void Bid(ActionEvent event) throws Exception {
        if (!SessionManager.getInstance().isLoggedIn()) return;
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/bid.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    public void Sell(ActionEvent event) throws Exception {
        if (!SessionManager.getInstance().isLoggedIn()) return;
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/sell.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    public void account(ActionEvent event) throws Exception {
        if (!SessionManager.getInstance().isLoggedIn()) return;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/account.fxml"));
        Parent root = loader.load();

        Stage popup = new Stage();
        popup.setTitle("My Account");
        popup.setScene(new Scene(root));
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.setResizable(false);
        popup.show();
    }

    public void logout(ActionEvent event) throws Exception {
        AutoBidManager.getInstance().cancelAll();
        SessionManager.getInstance().logout();
        SessionManager.getInstance().setCurrentUser(null);
        SessionManager.getInstance().setSessionId(null);
        ClientSocket.getInstance().close();
        ClientSocket.getInstance().reset();
        
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}