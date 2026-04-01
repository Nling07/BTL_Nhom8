package com.btl.n8.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class registerController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField rePasswordField;

    @FXML
    private void handleRegister(ActionEvent event) {

        try {

            Parent root = FXMLLoader.load(getClass().getResource("/home.fxml"));

            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public void goLogin(ActionEvent event) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }
}