package com.btl.n8.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;

public class loginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;


    public void handleLogin(ActionEvent event) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/home.fxml"));

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }

    public void goRegister(ActionEvent event) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/register.fxml"));

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }
}