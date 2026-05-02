package com.btl.n8.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class homeController {
    public void Bid(ActionEvent event) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/bid.fxml"));

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }
    public void Sell(ActionEvent event) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/sell.fxml"));

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }
    public void account(ActionEvent event) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/account.fxml"));

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }

}
