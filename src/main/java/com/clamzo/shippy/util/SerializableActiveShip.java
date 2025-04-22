package com.clamzo.shippy.util;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public class SerializableActiveShip {
    public UUID ownerId;
    public Location standLocation;
    public List<SerializedEntity> entities;
    public SerializedEntity helmEntity;

    public SerializableActiveShip(UUID ownerId, @NotNull Location location, List<SerializedEntity> entities, SerializedEntity helmEntity) {
        this.ownerId = ownerId;
        this.standLocation = location;
        this.entities = entities;
        this.helmEntity = helmEntity;
    }

    public static class SerializedEntity {
        public Location location;
        public ItemStack itemStack;
        public Vector3f transformation; // optional, or you can serialize full DisplayTransform

        public SerializedEntity() {}
        public SerializedEntity(Location loc, ItemStack item, Vector3f transform) {
            this.location = loc;
            this.itemStack = item;
            this.transformation = transform;
        }
    }
}

