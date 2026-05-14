package com.btl.n8.Controller;

import com.btl.n8.Model.entity.Admin;
import com.btl.n8.Model.entity.Bidder;
import com.btl.n8.Model.entity.Seller;
import com.btl.n8.Model.entity.User;
import com.btl.n8.Model.enums.Role;
import com.btl.n8.dto.LoginRequest;
import com.btl.n8.dto.LoginResponse;
import com.btl.n8.network.ClientSocket;
import com.btl.n8.network.ServerResponseListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javafx.application.Platform;
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

public class loginController implements ServerResponseListener {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private static final Gson gson = new Gson();
    private ActionEvent lastEvent;

    @FXML
    public void initialize() {
        ClientSocket.getInstance().addListener(this);
    }

    public void handleLogin(ActionEvent event) {
        this.lastEvent = event;
        messageLabel.setText("Connecting...");
        messageLabel.setVisible(true);

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username and password");
            messageLabel.setVisible(true);
            return;
        }

        // Chạy connect + send trên background thread — không block UI
        new Thread(() -> {
            if (!ClientSocket.getInstance().isConnected()) {
                boolean ok = ClientSocket.getInstance().connect();
                if (!ok) {
                    Platform.runLater(() -> {
                        messageLabel.setText("Cannot connect to server!");
                        messageLabel.setVisible(true);
                    });
                    return;
                }
            }
            // Gửi LoginRequest qua socket
            ClientSocket.getInstance().sendMessage(new LoginRequest(username, password));
        }).start();
    }

    @Override
    public void onRespone(JsonObject response) {
        String action = response.get("action").getAsString();
        if (!"LOGIN_SUCCESS".equals(action)) return;

        LoginResponse res = gson.fromJson(response, LoginResponse.class);

        Platform.runLater(() -> {
            if (!res.isSuccess()) {
                messageLabel.setText(res.getMessage());
                messageLabel.setVisible(true);
                return;
            }

            User user = buildUser(res);
            SessionManager.getInstance().setCurrentUser(user);
            SessionManager.getInstance().setSessionId(res.getSessionId());
            ClientSocket.getInstance().removeListener(this);

            try {
                String fxml = res.getRole() == Role.ADMIN
                        ? "/fxml/admin.fxml"
                        : "/fxml/home.fxml";
                Parent root = FXMLLoader.load(getClass().getResource(fxml));
                Stage stage = (Stage)((Node)lastEvent.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private User buildUser(LoginResponse res) {
        return switch (res.getRole()) {
            case BIDDER -> new Bidder(res.getUserId(), res.getUsername(), "",
                    res.getBalance() != null ? res.getBalance() : BigDecimal.ZERO);
            case SELLER -> new Seller(res.getUserId(), res.getUsername(), "");
            default     -> new Admin(res.getUserId(), res.getUsername(), "");
        };
    }

    public void goRegister(ActionEvent event) throws Exception {
        ClientSocket.getInstance().removeListener(this);
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}