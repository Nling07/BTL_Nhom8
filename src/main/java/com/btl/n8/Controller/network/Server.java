package com.btl.n8.Controller.network;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket serverSocket;
    private DataInputStream in;
    public static final int PORT = 9090;

    public Server(){
        try{
            serverSocket = new ServerSocket(PORT);
            initConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initConnection() throws IOException{
        Socket clientSocket = serverSocket.accept();
        in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
        readMessage();
        close();
    }

    private void close() throws IOException{
        in.close();
        serverSocket.close();
    }

    private void readMessage()throws IOException{
        String line = "";
        while (line != null){
            line = in.readUTF();
            System.out.println(line);
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}
