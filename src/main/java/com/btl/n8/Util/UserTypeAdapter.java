package com.btl.n8.Util;

import com.btl.n8.Model.Entity.Admin;
import com.btl.n8.Model.Entity.Bidder;
import com.btl.n8.Model.Entity.Seller;
import com.btl.n8.Model.Entity.User;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Gson TypeAdapter để deserialize abstract class User thành đúng subclass
 * (Bidder / Seller / Admin) dựa theo field "role" trong JSON.
 *
 * Đăng ký vào GsonBuilder:
 *   new GsonBuilder()
 *       .registerTypeAdapter(User.class, new UserTypeAdapter())
 *       .create();
 *
 * BẮT BUỘC ở mọi nơi dùng Gson để parse JSON có chứa User:
 *   - HomeController       (UpgradeSellerResponse chứa User updatedUser)
 *   - AdminController      (AdminResponse chứa List<User>)
 *   - RequestHandler       (serialize phía server)
 */
public class UserTypeAdapter implements JsonDeserializer<User> {

    @Override
    public User deserialize(JsonElement json, Type typeOfT,
                            JsonDeserializationContext ctx) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        String role = "BIDDER";
        if (obj.has("role") && !obj.get("role").isJsonNull()) {
            role = obj.get("role").getAsString();
        }

        switch (role) {
            case "ADMIN":
                return ctx.deserialize(obj, Admin.class);
            case "SELLER":
                return ctx.deserialize(obj, Seller.class);
            case "BIDDER":
            default:
                return ctx.deserialize(obj, Bidder.class);
        }
    }
}
