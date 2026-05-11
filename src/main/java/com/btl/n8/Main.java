package com.btl.n8;

import com.btl.n8.Connection.*;
import com.btl.n8.Model.*;
import com.btl.n8.Network.ClientHandler;
import com.btl.n8.Network.ClientSocket;
import com.btl.n8.Service.*;
import com.mysql.cj.jdbc.ConnectionImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        ClientSocket.getInstance();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));

        stage.setTitle("Pokemon Auction System");
        stage.setScene(new Scene(loader.load()));
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) throws SQLException {
        launch();
    }
}