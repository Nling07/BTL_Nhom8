package com.btl.n8.util;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public JsonElement serialize(LocalDateTime src, Type type, JsonSerializationContext ctx) {
        return new JsonPrimitive(src.format(FMT));
    }

    @Override
    public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) {
        return LocalDateTime.parse(json.getAsString(), FMT);
    }
}