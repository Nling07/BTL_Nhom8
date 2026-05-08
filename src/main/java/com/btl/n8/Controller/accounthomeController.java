package com.btl.n8.Controller;

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

    @FXML
    private Label userInfoLabel;

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            String userInfo = String.format("Welcome, %s\nRole: %s\nID: %d",
                    user.getAccount(), user.getRole(), user.getId());
            userInfoLabel.setText(userInfo);
        } else {
            userInfoLabel.setText("Error: User not logged in");
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
