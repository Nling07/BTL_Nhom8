package com.btl.n8.Controller.network;

import com.btl.n8.Controller.dto.Request;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSocket{
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private static final String  SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 9090;
/*
    public ClientSocket()  {
        try {
            this.socket = new Socket(SERVER_IP,SERVER_PORT);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(),true);
            startListening();
        } catch (IOException e) {
            System.out.println("Server chua mở máy chủ");;
        }
    }
    //Controller phải tạo request object
    // phần này tạo thêm 1 luồng mới để parse String sang json
    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try{
                String json;
                while ((json = in.readLine()) != null){
                    JsonObject object = JsonParser.parseString(json).getAsJsonObject(); //parse String json thành element rồi biến nó thành json obj
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

 */
}
