package com.clamzo.shippy.util;

import com.clamzo.shippy.behavior.ShipController;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ActiveShip {
    private final UUID ownerId;
    private final ArmorStand standEntity;
    private final List<Entity> entities;
    private final ShipController controller;
    private List<BoundingBox> cachedBoundingBoxes;

    public ActiveShip(UUID ownerId, ArmorStand standEntity, List<Entity> entities) {
        this.ownerId = ownerId;
        this.standEntity = standEntity;
        this.entities = entities;
        this.controller = new ShipController(this);
    }

    //    public void updateBoundingBoxes() {
//        cachedBoundingBoxes = entities.stream().filter(el -> el instanceof BlockDisplay)
//                .map(Entity::getBoundingBox)
//                .collect(Collectors.toList());
//    }
    public void updateBoundingBoxes() {
        Location standLocation = this.standEntity.getLocation();
        float yawDegrees = this.standEntity.getYaw();
        cachedBoundingBoxes = calculateBoundingBoxes(yawDegrees, standLocation);
    }

    public @NotNull List<BoundingBox> calculateBoundingBoxes(float yawDegrees, Location standLocation) {
        return entities.stream()
                .filter(e -> e instanceof BlockDisplay)
                .map(e -> {
                    BlockDisplay display = (BlockDisplay) e;
                    Transformation transformation = display.getTransformation();
                    Vector3f relativeTranslation = transformation.getTranslation(); // relative to the armor stand

                    // Convert yaw to radians and compute rotation
                    double yaw = Math.toRadians(yawDegrees); // Negative to match Minecraft's rotation
                    double cos = Math.cos(yaw);
                    double sin = Math.sin(yaw);

                    // Rotate the translation vector around the Y-axis
                    double x = relativeTranslation.x;
                    double z = relativeTranslation.z;
                    Vector rotated = new Vector(
                            x * cos - z * sin,
                            relativeTranslation.y,
                            -x * sin + z * cos
                    );

                    // Final world position of the BlockDisplay
                    Vector worldPosition = standLocation.toVector().add(rotated);

                    // Snap to nearest block position
                    int bx = (int) Math.floor(worldPosition.getX());
                    int by = (int) Math.floor(worldPosition.getY());
                    int bz = (int) Math.floor(worldPosition.getZ());

                    // Create 1x1x1 AABB for the block
                    return new BoundingBox(bx, by, bz, bx + 1, by + 1, bz + 1);
                })
                .collect(Collectors.toList());
    }


    public UUID getOwnerId() {
        return ownerId;
    }

    public ArmorStand getStandEntity() {
        return standEntity;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public ShipController getController() {
        return controller;
    }

    public List<BoundingBox> getBoundingBoxes() {
        return cachedBoundingBoxes;
    }
}


