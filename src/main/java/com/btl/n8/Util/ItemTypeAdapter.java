package com.btl.n8.Util;

import com.btl.n8.Model.Entity.Card;
import com.btl.n8.Model.Entity.Figure;
import com.btl.n8.Model.Entity.Item;
import com.btl.n8.Model.Entity.Poster;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Gson TypeAdapter để deserialize abstract class Item thành đúng subclass
 * (Figure / Card / Poster) dựa theo field "type" trong JSON.
 *
 * Đăng ký vào GsonBuilder:
 *   new GsonBuilder()
 *       .registerTypeAdapter(Item.class, new ItemTypeAdapter())
 *       .create();
 *
 * BẮT BUỘC ở mọi nơi dùng Gson để parse JSON có chứa Item hoặc List<Item>:
 *   - SellController     (GetSellerItemsResponse chứa List<Item>)
 *   - AdminController    (AdminResponse chứa List<Item>)
 *   - RequestHandler     (serialize phía server)
 */
public class ItemTypeAdapter implements JsonDeserializer<Item> {

    @Override
    public Item deserialize(JsonElement json, Type typeOfT,
                            JsonDeserializationContext ctx) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        String type = "FIGURE"; // default
        if (obj.has("type") && !obj.get("type").isJsonNull()) {
            type = obj.get("type").getAsString();
        }

        switch (type) {
            case "CARD":   return ctx.deserialize(obj, Card.class);
            case "POSTER": return ctx.deserialize(obj, Poster.class);
            case "FIGURE":
            default:       return ctx.deserialize(obj, Figure.class);
        }
    }
}
