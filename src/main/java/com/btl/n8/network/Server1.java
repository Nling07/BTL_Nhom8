package com.btl.n8.Network;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server1 {
    private ServerSocket server;
    private boolean isListening = true;
    private static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private ExecutorService pool = Executors.newFixedThreadPool(10);
    private static final int PORT = 9090;

    public Server1() throws IOException {
        this.server = new ServerSocket(PORT);
        System.out.println("Server dang chay");

        while (isListening){
            Socket clientSocket = server.accept();
            System.out.println("Clien moi ket noi: " + clientSocket.getInetAddress());
            try{
                ClientHandler clientHandler = new ClientHandler(clientSocket,clients);
                clients.add(clientHandler);
                pool.execute(clientHandler);

            } catch (IOException e) {
                System.err.println("Khong the tao clienthandler cho client " + e.getMessage());
                clientSocket.close();
            }
        }
    }
    public static void main(String[] args) throws IOException {
        new Server1();
    }
}
