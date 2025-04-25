package com.clamzo.shippy.serialization;

import com.google.gson.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.lang.reflect.Type;
import java.util.Map;

public class ItemStackAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
    @Override
    public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext ctx) {
        // ItemStack is ConfigurationSerializable, so serialize its map
        Map<String,Object> map = src.serialize();
        return ctx.serialize(map);
    }
    @Override
    public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {
        // deserialize back into the map, then into ItemStack
        Map<?,?> map = ctx.deserialize(json, Map.class);
        return ItemStack.deserialize((Map<String, Object>) map);
    }
}

