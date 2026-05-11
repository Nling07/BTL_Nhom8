package com.btl.n8.DTO;

import com.google.gson.Gson;

public class JsonUtil {

    private static final Gson gson = new Gson();

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> tclass) {
        return gson.fromJson(json, tclass);
    }
}
