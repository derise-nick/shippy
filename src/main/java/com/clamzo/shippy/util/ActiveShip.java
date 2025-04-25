package com.clamzo.shippy.util;

import com.clamzo.shippy.behavior.ShipController;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ActiveShip implements InventoryHolder {
    private final UUID ownerId;
    private final ArmorStand standEntity;
    private final List<Entity> entities;
    private final ShipController controller;
    private @NotNull List<OrientedBoundingBox> cachedBoundingBoxes;
    private final Map<UUID, BlockDisplay> cannons;
    private final Inventory inventory;

    public ActiveShip(UUID ownerId, ArmorStand standEntity, List<Entity> entities, Map<UUID, BlockDisplay> cannons) {
        this.ownerId = ownerId;
        this.standEntity = standEntity;
        this.entities = entities;
        this.controller = new ShipController(this);
        this.cannons = cannons;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Hold"));
    }

    public void updateBoundingBoxes() {
        Location standLocation = this.standEntity.getLocation();
        float yawDegrees = this.standEntity.getYaw();
        cachedBoundingBoxes = calculateOBBs(yawDegrees, standLocation);
    }

    public @NotNull List<OrientedBoundingBox> calculateOBBs(
            float yawDegrees, Location standLocation) {

        return entities.stream()
                .filter(e -> e instanceof BlockDisplay)
                .map(e -> {
                    BlockDisplay d = (BlockDisplay) e;
                    Vector3f t = d.getTransformation().getTranslation().add(0.5f,0,0.5f);
                    double radians = Math.toRadians(yawDegrees);
                    double cos = Math.cos(radians), sin = Math.sin(radians);
                    double x = t.x()*cos - t.z()*sin;
                    double z = t.x()*sin + t.z()*cos;
                    Vector center = standLocation.toVector()
                            .add(new Vector(x, t.y()+0.5, z));

                    Vector half = new Vector(0.5, 0.5, 0.5);

                    return new OrientedBoundingBox(center, half, yawDegrees);
                })
                .collect(Collectors.toList());
    }


    public @NotNull List<BoundingBox> calculateBoundingBoxes(float yawDegrees, Location standLocation) {
        return entities.stream()
                .filter(e -> e instanceof BlockDisplay)
                .map(e -> {
                    BlockDisplay display = (BlockDisplay) e;
                    Transformation transformation = display.getTransformation();
                    Vector3f relativeTranslation = transformation.getTranslation(); // relative to the armor stand

                    // Convert yaw to radians and compute rotation
                    double yaw = Math.toRadians(yawDegrees);
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

    public List<OrientedBoundingBox> getBoundingBoxes() {
        return cachedBoundingBoxes;
    }

    public Map<UUID, BlockDisplay> getCannons() {
        return cannons;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}


