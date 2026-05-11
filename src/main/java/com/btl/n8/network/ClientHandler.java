package com.btl.n8.Network;

import com.btl.n8.Connection.*;
import com.btl.n8.DTO.*;
import com.btl.n8.Model.Role;
import com.btl.n8.Model.User;
import com.btl.n8.Service.AuctionService;
import com.btl.n8.Service.BidService;
import com.btl.n8.Service.ItemService;
import com.btl.n8.Service.UserService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.sql.Connection;
import java.util.concurrent.CopyOnWriteArrayList;


/*FLow chuyển data:
Client nhận đối tượng request tạo từ controller
Client gửi lên server thông qua việc biến đối tượng thành chuỗi json
ở Server, ClientHander chuyển chuỗi json thành đối tượng java để xử lí dữ liệu
Trong ClientHandler sẽ gọi các service kết nối cùng với database để xử lí
Khi xử lí xong thì Server sẽ trả về Client 1 respone cùng loại (Ví dụ gửi login request -> login response)
Client sau khi nhận được respone(chuỗi json) thì chuyển chuổi json đó thành JsonObject
Sau khi chuyển thành JsonOject thì nó gửi đến các Listener (Controller) để xem controller nào cần thì sử dụng
Nôm na là kiểu design pattern: Observer pattern
*/
public class ClientHandler implements Runnable{
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private static CopyOnWriteArrayList<ClientHandler> clients;
    private static final Gson gson = new Gson();

    private AuctionService auctionService;
    private UserService userService;
    private BidService bidService;
    private ItemService itemService;

    public ClientHandler(Socket clientSocket, CopyOnWriteArrayList<ClientHandler> clients) throws IOException {
        this.socket = clientSocket;
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(),true);
        this.clients = clients;
        try{
            Connection connection = DataConnection.getConnection();
            this.userService = new UserService(new UserDAOImpl(connection));
            this.bidService = new BidService(new BidDAOImpl(connection));
            this.itemService = new ItemService(new ItemDAOImpl(connection));

        } catch (Exception e) {
            System.err.println("Không thể khởi tạo Service: ");
        }
    }

    @Override
    public void run() {
        try{
            String json;
            while ((json = in.readLine()) != null){
                try {
                    JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                    String action = jsonObject.get("action").getAsString();
                    if (action.equals("BID")){
                        BidRequest bidRequest = gson.fromJson(json,BidRequest.class);
                        handleBid(bidRequest);
                    }
                    else if (action.equals("LOGIN")){
                        LoginRequest loginRequest = gson.fromJson(json,LoginRequest.class);
                        handleLogin(loginRequest);
                    }
                    else if (action.equals("REGISTER")){
                        RegisterRequest registerRequest = gson.fromJson(json,RegisterRequest.class);
                        handleRegister(registerRequest);
                    }
                    else if(action.equals("ADD_ITEM")){
                        AddItemRequest addItemRequest = gson.fromJson(json,AddItemRequest.class);
                        handleAddItemRequest(addItemRequest);
                    }

                } catch (JsonSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleAddItemRequest(AddItemRequest addItemRequest) {

    }

    private void handleRegister(RegisterRequest registerRequest) {
    }
            //
    private void handleLogin(LoginRequest loginRequest) {
        // xử lí login request từ client, nếu đúng thì trả về respone
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        User user = userService.login(username,password);
        //Kiểm tra user có trong database không, nếu có thì user.getRole để xem là role nó là gì
        user.getRole();
        if (user == null) {
            out.println(gson.toJson(new LoginResponse("LOGIN_FAIL", false, -1, null, null, null)));
            return;
        }
        else if (user.getRole().equals(Role.ADMIN)){

        }
        else if (user.getRole().equals(Role.BIDDER) && user.getRole().equals(Role.SELLER) ){
            LoginResponse loginResponse = new LoginResponse("Dang nhap thanh cong", true,user.getId(),
                    user.getAccount(),
                    user.getRole(),user.getBalance());

        }
        /*if (user == null){
            out.println(gson.toJson(new LoginResponse("LOGIN_FAIL",false, -1,null,null,null)));
            return;
        }
        LoginResponse loginResponse = new LoginResponse("Dang nhap thanh cong ", true,user.getId(),
                                            user.getAccount(),
                                            user.getRole(),user.getBalance());
                                            String jsonres = gson.toJson(loginResponse);
        out.println(jsonres);
        */
    }

    private void handleBid(BidRequest bidRequest) {
        int auctionId = bidRequest.getAuctionId();
        BigDecimal amount = bidRequest.getAmount();
        boolean ok = bidService.placeBid(auctionId,bidRequest.getBidderId(), amount);
        if (!ok){
            out.println(gson.toJson(new BidResponse("Bid Fail",false, auctionId,amount)));
            return;
        }
        auctionService.placeBid(auctionId,amount);
        BidResponse bidResponse = new BidResponse("Dat gio thanh cong",true,auctionId,amount);
        String jsonres = gson.toJson(bidResponse);
        out.println(jsonres);
    }


    public void broadcastAll(String msg){
        for (ClientHandler aClient : clients){
                if(aClient == this){
                    continue;
                }
                aClient.out.println(msg);
        }
    }
}
