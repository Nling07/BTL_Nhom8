package com.btl.n8.DTO;

import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Item;
import com.btl.n8.Model.Entity.User;

import java.util.List;

public class AdminResponse extends Response {
    private boolean       success;
    // data fields — chỉ 1 cái được dùng tùy action
    private List<User>    users;
    private List<Auction> auctions;
    private List<Item>    items;
    // dashboard counters
    private int totalUsers, totalAuctions, openAuctions,
            totalItems, totalSellers, cancelledAuctions;

    public AdminResponse() { super(); }

    public AdminResponse(String action, String message, String sessionId, boolean success) {
        super(action, message, sessionId);
        this.success = success;
    }

    public boolean       isSuccess()             { return success; }
    public List<User>    getUsers()              { return users; }
    public List<Auction> getAuctions()           { return auctions; }
    public List<Item>    getItems()              { return items; }
    public int getTotalUsers()                   { return totalUsers; }
    public int getTotalAuctions()                { return totalAuctions; }
    public int getOpenAuctions()                 { return openAuctions; }
    public int getTotalItems()                   { return totalItems; }
    public int getTotalSellers()                 { return totalSellers; }
    public int getCancelledAuctions()            { return cancelledAuctions; }

    public AdminResponse withUsers(List<User> u)       { this.users = u; return this; }
    public AdminResponse withAuctions(List<Auction> a) { this.auctions = a; return this; }
    public AdminResponse withItems(List<Item> i)       { this.items = i; return this; }
    public AdminResponse withDashboard(int tu, int ta, int oa, int ti, int ts, int ca) {
        this.totalUsers=tu; this.totalAuctions=ta; this.openAuctions=oa;
        this.totalItems=ti; this.totalSellers=ts; this.cancelledAuctions=ca;
        return this;
    }
}
