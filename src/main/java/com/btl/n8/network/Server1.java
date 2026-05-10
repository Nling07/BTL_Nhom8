package com.btl.n8.network;

import com.mysql.cj.xdevapi.Client;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server1 {
    private ServerSocket serversocket;
    private InputStreamReader in;
    private PrintWriter out;
    private Socket socket;
    private static ArrayList<ClientHandler> clients = new ArrayList<>();
    private ExecutorService pool = Executors.newFixedThreadPool(4);
    private static final int PORT = 9090;
    public Server1() throws IOException {
        ServerSocket listener = new ServerSocket(PORT);

        while (true){
            Socket client = listener.accept();
            ClientHandler clientThread = new ClientHandler(client,clients);
            clients.add(clientThread);

            pool.execute(clientThread);

        }


    }
}

