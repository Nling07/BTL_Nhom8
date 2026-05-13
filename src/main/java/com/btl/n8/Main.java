package com.btl.n8;

import com.btl.n8.network.ClientSocket;
import com.btl.n8.network.Server1;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        ClientSocket.getInstance();

        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource("/fxml/login.fxml")
                );

        stage.setTitle("Pokemon Auction System");

        stage.setScene(new Scene(loader.load()));

        stage.setResizable(false);

        stage.show();
    }

    public static void main(String[] args) {

        new Thread(() -> {

            try {

                Server1.main(new String[]{});

            } catch (Exception e) {

                System.out.println(
                        "Failed to start server"
                );

                e.printStackTrace();
            }

        }).start();

        launch();
    }
}