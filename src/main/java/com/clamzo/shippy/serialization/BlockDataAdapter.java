package com.clamzo.shippy.serialization;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

import java.lang.reflect.Type;

public class BlockDataAdapter implements JsonSerializer<BlockData>, JsonDeserializer<BlockData> {
    @Override
    public JsonElement serialize(BlockData src, Type typeOfSrc, JsonSerializationContext ctx) {
        return new JsonPrimitive(src.getAsString());
    }
    @Override
    public BlockData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {
        return Bukkit.createBlockData(json.getAsString());
    }
}
