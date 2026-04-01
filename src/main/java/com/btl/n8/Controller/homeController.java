package com.btl.n8.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class homeController {
    public void Bid(ActionEvent event) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/bid.fxml"));

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }
    public void Sell(ActionEvent event) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/sell.fxml"));

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }
    public void account(ActionEvent event) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/account.fxml"));

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }

}
