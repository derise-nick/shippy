package com.clamzo.shippy.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.lang.reflect.Type;
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

    public static class SerializedEntity<T extends Entity> {
        public Location location;
        public ItemStack itemStack;
        public Vector3f transformation;
        public String entityType;


        public SerializedEntity(Location loc, String type) {
            this.location = loc;
            this.entityType = type;
        }

        public SerializedEntity(Location loc, Vector3f transformation, String type) {
            this.location = loc;
            this.transformation = transformation;
            this.entityType = type;
        }

        public SerializedEntity(Location loc, ItemStack item, Vector3f transform) {
            this.location = loc;
            this.itemStack = item;
            this.transformation = transform;
            this.entityType = ItemDisplay.class.toString();
        }
    }
}

