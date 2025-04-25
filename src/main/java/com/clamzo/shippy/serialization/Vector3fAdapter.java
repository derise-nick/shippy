package com.clamzo.shippy.serialization;

import com.google.gson.*;
import org.joml.Vector3f;

import java.lang.reflect.Type;

public class Vector3fAdapter implements JsonSerializer<Vector3f>, JsonDeserializer<Vector3f> {
    @Override
    public JsonElement serialize(Vector3f src, Type typeOfSrc, JsonSerializationContext ctx) {
        JsonObject o = new JsonObject();
        o.addProperty("x", src.x());
        o.addProperty("y", src.y());
        o.addProperty("z", src.z());
        return o;
    }
    @Override
    public Vector3f deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {
        JsonObject o = json.getAsJsonObject();
        return new Vector3f(
                o.get("x").getAsFloat(),
                o.get("y").getAsFloat(),
                o.get("z").getAsFloat()
        );
    }
}

