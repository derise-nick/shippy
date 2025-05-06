package com.clamzo.shippy.util;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public class SerializableActiveShip {
    public UUID ownerId;
    public Location standLocation;
    public List<SerializedEntity> entities;
    public int helmHeight;

    public SerializableActiveShip(UUID ownerId, @NotNull Location location, List<SerializedEntity> entities, int helmHeight) {
        this.ownerId = ownerId;
        this.standLocation = location;
        this.entities = entities;
        this.helmHeight = helmHeight;
    }

    public static class SerializedEntity {
        public Quaternionf rotation;
        public Location location;
        public byte[] itemStack;
        public Vector3f translation;
        public @NotNull EntityType entityType;
        public String blockData;


        public SerializedEntity(Location loc, @NotNull EntityType type) {
            this.location = loc;
            this.entityType = type;
        }

        public SerializedEntity(Location loc, Vector3f translation, @NotNull EntityType type) {
            this.location = loc;
            this.translation = translation;
            this.entityType = type;
        }

        public SerializedEntity(Location loc, Vector3f translation, BlockData blockData) {
            this.location = loc;
            this.translation = translation;
            this.blockData = blockData.getAsString();
            this.entityType = EntityType.BLOCK_DISPLAY;
        }

        public SerializedEntity(Location loc, ItemStack item, Vector3f translation, Quaternionf rotation) {
            this.location = loc;
            this.itemStack = item.serializeAsBytes();
            this.translation = translation;
            this.entityType = EntityType.ITEM_DISPLAY;
            this.rotation = rotation;
        }
    }
}

