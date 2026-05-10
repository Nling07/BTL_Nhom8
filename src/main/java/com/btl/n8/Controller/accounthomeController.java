package com.btl.n8.Controller;

import com.btl.n8.Model.Bidder;
import com.btl.n8.Model.User;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class accounthomeController {

    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Label idLabel;
    @FXML private Label balanceLabel;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            usernameLabel.setText("Error: Not logged in");
            return;
        }

        usernameLabel.setText(user.getAccount());
        roleLabel.setText(user.getRole().name());
        idLabel.setText("#" + user.getId());

        if (user instanceof Bidder bidder) {
            balanceLabel.setText(String.format("%,.0f ₫", bidder.getBalance()));
        } else {
            balanceLabel.setText("-");
        }
    }

    @FXML
    public void Return(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/home.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}