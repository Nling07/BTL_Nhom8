package com.btl.n8.Controller;

import com.btl.n8.Connection.DataConnection;
import com.btl.n8.Connection.UserDAOImpl;
import com.btl.n8.Model.Bidder;
import com.btl.n8.Model.Role;
import com.btl.n8.Model.Seller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.math.BigDecimal;
import java.sql.Connection;

public class registerController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField rePasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private void handleRegister(ActionEvent event) {
        messageLabel.setText("");
        messageLabel.setVisible(false);

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String rePassword = rePasswordField.getText().trim();

        // Validation
        if (username.isEmpty()) {
            showError("Please enter username");
            return;
        }
        if (username.length() < 3) {
            showError("Username must be at least 3 characters");
            return;
        }
        if (password.isEmpty()) {
            showError("Please enter password");
            return;
        }
        if (rePassword.isEmpty()) {
            showError("Please confirm password");
            return;
        }
        if (!password.equals(rePassword)) {
            showError("Passwords do not match");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        // For simplicity, register as BIDDER with default balance 0
        Bidder user = new Bidder(username, password, BigDecimal.ZERO);

        Task<Void> registerTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = DataConnection.getConnection()) {
                    if (conn == null) {
                        throw new Exception("Database connection failed");
                    }
                    UserDAOImpl userDAO = new UserDAOImpl(conn);
                    boolean success = userDAO.insert(user);
                    if (!success) {
                        throw new Exception("Username already exists");
                    }
                }
                return null;
            }
        };

        registerTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                try {
                    goLogin(event);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });

        registerTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                Throwable exception = registerTask.getException();
                messageLabel.setText(exception.getMessage() != null ? exception.getMessage() : "Registration failed");
                messageLabel.setVisible(true);
            });
        });

        new Thread(registerTask).start();
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
    }

    public void goLogin(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}