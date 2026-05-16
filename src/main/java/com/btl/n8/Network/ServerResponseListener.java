package com.btl.n8.Network;

import com.google.gson.JsonObject;

public interface ServerResponseListener {
    //Observer
    // gọi hàm khi có dữ liệu trả về từ Server
    void onRespone(JsonObject respone);
}
