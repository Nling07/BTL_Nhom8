package com.btl.n8.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class homeController {

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            System.out.println("Warning: User not logged in");
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
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/account.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    public void logout(ActionEvent event) throws Exception {
        SessionManager.getInstance().logout();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}