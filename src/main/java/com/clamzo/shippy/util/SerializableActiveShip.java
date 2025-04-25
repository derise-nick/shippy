package com.clamzo.shippy.util;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SerializableActiveShip {
    public UUID ownerId;
    public Location standLocation;
    public List<SerializedEntity> entities;

    public SerializableActiveShip(UUID ownerId, @NotNull Location location, List<SerializedEntity> entities) {
        this.ownerId = ownerId;
        this.standLocation = location;
        this.entities = entities;
    }

    public static class SerializedEntity {
        public Location location;
        public byte[] itemStack;
        public Vector3f transformation;
        public @NotNull EntityType entityType;
        public String blockData;


        public SerializedEntity(Location loc, @NotNull EntityType type) {
            this.location = loc;
            this.entityType = type;
        }

        public SerializedEntity(Location loc, Vector3f transformation, @NotNull EntityType type) {
            this.location = loc;
            this.transformation = transformation;
            this.entityType = type;
        }

        public SerializedEntity(Location loc, Vector3f transformation, BlockData blockData) {
            this.location = loc;
            this.transformation = transformation;
            this.blockData = blockData.getAsString();
            this.entityType = EntityType.BLOCK_DISPLAY;
        }

        public SerializedEntity(Location loc, ItemStack item, Vector3f transform) {
            this.location = loc;
            this.itemStack = item.serializeAsBytes();
            this.transformation = transform;
            this.entityType = EntityType.ITEM_DISPLAY;
        }
    }
}

