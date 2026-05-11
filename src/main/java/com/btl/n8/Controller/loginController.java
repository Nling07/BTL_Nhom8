package com.btl.n8.Controller;

import com.btl.n8.Connection.DataConnection;
import com.btl.n8.Connection.UserDAOImpl;
import com.btl.n8.DTO.LoginRequest;
import com.btl.n8.Model.Role;
import com.btl.n8.Model.User;

import com.btl.n8.Network.ClientSocket;
import com.btl.n8.Network.ServerResponseListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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

import java.sql.Connection;

public class loginController implements ServerResponseListener {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private static final Gson gson = new Gson();
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

        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                Connection conn = DataConnection.getConnection();
                if (conn == null) throw new Exception("Database connection failed");
                UserDAOImpl userDAO = new UserDAOImpl(conn);
                User user = userDAO.findByAccount(username);
                if (user != null && user.getPassword().equals(password)) {
                    return user;
                } else {
                    throw new Exception("Invalid username or password");
                }
            }
        };

        loginTask.setOnSucceeded(e -> Platform.runLater(() -> {
            User user = loginTask.getValue();
            SessionManager.getInstance().setCurrentUser(user);
            try {
                // Admin → admin.fxml, others → home.fxml
                String fxml = user.getRole() == Role.ADMIN
                        ? "/fxml/admin.fxml"
                        : "/fxml/home.fxml";
                Parent root = FXMLLoader.load(getClass().getResource(fxml));
                Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }));

        loginTask.setOnFailed(e -> Platform.runLater(() -> {
            messageLabel.setText(loginTask.getException().getMessage() != null
                    ? loginTask.getException().getMessage()
                    : "Login failed. Please try again.");
            messageLabel.setVisible(true);
        }));

        new Thread(loginTask).start();
        //tạo request để gửi đến client (thường thì ở mỗi controller sẽ tạo một cái)..
        LoginRequest loginRequest = new LoginRequest(username,password);
        ClientSocket.getInstance().sendMessage(loginRequest);


    }

    public void goRegister(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    /*đây là override hàm trong observer patter, về cơ bản hàm này sẽ xử lí JsonObject mà ở Client đã chuyển dạng đối tượng
    respone (java) sang JsonObject để xử lí. phần này sẽ được tích hợp với controller nhé..
    */
    @Override
    public void onRespone(JsonObject respone) {

    }
}