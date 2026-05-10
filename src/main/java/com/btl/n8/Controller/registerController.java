package com.btl.n8.Controller;

import com.btl.n8.Connection.DataConnection;
import com.btl.n8.Connection.UserDAOImpl;
import com.btl.n8.Model.Bidder;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.Connection;

public class registerController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField rePasswordField;
    @FXML private Label messageLabel;

    @FXML
    private void handleRegister(ActionEvent event) {
        messageLabel.setText("");
        messageLabel.setVisible(false);

        String username   = usernameField.getText().trim();
        String password   = passwordField.getText().trim();
        String rePassword = rePasswordField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || rePassword.isEmpty()) {
            showError("Please fill all fields"); return;
        }
        if (username.length() < 3) {
            showError("Username must be at least 3 characters"); return;
        }
        if (!password.equals(rePassword)) {
            showError("Passwords do not match"); return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters"); return;
        }

        Bidder newUser = new Bidder(username, password, BigDecimal.ZERO);

        Task<Void> registerTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Connection conn = DataConnection.getConnection();
                if (conn == null) throw new Exception("Database connection failed");
                UserDAOImpl userDAO = new UserDAOImpl(conn);
                boolean success = userDAO.insert(newUser);
                if (!success) throw new Exception("Username already exists");
                return null;
            }
        };

        registerTask.setOnSucceeded(e -> Platform.runLater(() -> {
            try { goLogin(event); } catch (Exception ex) { ex.printStackTrace(); }
        }));

        registerTask.setOnFailed(e -> Platform.runLater(() ->
                showError(registerTask.getException().getMessage() != null
                        ? registerTask.getException().getMessage()
                        : "Registration failed. Please try again.")
        ));

        new Thread(registerTask).start();
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
    }

    public void goLogin(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}