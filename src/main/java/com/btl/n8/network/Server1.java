package com.btl.n8.Network;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.*;

public class Server1 {
    private ServerSocket server;
    private boolean isListening = true;
    private static ConcurrentHashMap<String,ClientHandler> clients= new ConcurrentHashMap<>();
    private ExecutorService pool = Executors.newFixedThreadPool(10);
    private static final int PORT = 9090;
    private String token;

    public Server1() throws IOException {
        this.server = new ServerSocket(PORT);
        System.out.println("Server dang chay");

        while (isListening){
            Socket clientSocket = server.accept();
            System.out.println("Clien moi ket noi: " + clientSocket.getInetAddress());
            try{
                ClientHandler clientHandler = new ClientHandler(clientSocket,clients);
                clients.put(token,clientHandler);
                pool.execute(clientHandler);

            } catch (IOException e) {
                System.err.println("Khong the tao clienthandler cho client " + e.getMessage());
                clientSocket.close();
            }
        }
    }
    //
    public static void main(String[] args) throws IOException {
        new Server1();
    }
}
