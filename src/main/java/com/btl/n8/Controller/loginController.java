package com.btl.n8.Controller;

import com.btl.n8.Connection.DataConnection;
import com.btl.n8.Connection.UserDAOImpl;
import com.btl.n8.Model.User;

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

import java.sql.Connection;

public class loginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    public void handleLogin(ActionEvent event) {
        messageLabel.setText("");
        messageLabel.setVisible(false);

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username and password");
            messageLabel.setVisible(true);
            return;
        }

        Task<User> loginTask = new Task<User>() {
            @Override
            protected User call() throws Exception {
                try (Connection conn = DataConnection.getConnection()) {
                    UserDAOImpl userDAO = new UserDAOImpl(conn);
                    User user = userDAO.findByAccount(username);
                    if (user != null && user.getPassword().equals(password)) {
                        return user;
                    } else {
                        throw new Exception("Invalid username or password");
                    }
                }
            }
        };

        loginTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                User user = loginTask.getValue();
                SessionManager.setCurrentUser(user);
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/fxml/home.fxml"));
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });

        loginTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                Throwable exception = loginTask.getException();
                messageLabel.setText(exception.getMessage() != null ? exception.getMessage() : "Login failed. Please try again.");
                messageLabel.setVisible(true);
            });
        });

        new Thread(loginTask).start();
    }

    public void goRegister(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}