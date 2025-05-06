package com.clamzo.shippy.ship;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.util.OrientedBoundingBox;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

public class ActiveShip implements InventoryHolder {
    private final UUID ownerId;
    private final ArmorStand standEntity;
    private final List<Entity> entities;
    private final ShipController controller;
    private final ShippyPlugin plugin;
    private @NotNull List<OrientedBoundingBox> cachedBoundingBoxes;
    private final Inventory inventory;
    private final int helmHeight;
    private final Map<UUID, Cannon> cannons;
    private final Map<UUID, BlockDisplay> interactionRefs;

    public ActiveShip(UUID ownerId, ArmorStand standEntity, List<Entity> entities, Map<UUID, BlockDisplay> interactionRefs, int helmHeight, ShippyPlugin plugin) {
        this.plugin = plugin;
        this.ownerId = ownerId;
        this.standEntity = standEntity;
        this.entities = entities;
        this.interactionRefs = interactionRefs;
        this.controller = new ShipController(this);
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Hold"));
        this.helmHeight = helmHeight;
        this.cannons = new HashMap<>();
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

    public Map<UUID, Cannon> getCannons() {
        return cannons;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public int getHelmHeight() {
        return helmHeight;
    }

    public void addCannon(Interaction interaction) {
        cannons.put(interaction.getUniqueId(), new Cannon(this.ownerId, interaction.getUniqueId(), 10, 30));
    }

    public boolean fireCannon(UUID cannonId) {
        if (!cannons.containsKey(cannonId))  {
            plugin.getLogger().warning("Attempted to fire unregistered cannon! Id is: " + cannonId);
            return false;
        }
        return cannons.get(cannonId).fire();
    }

    public Map<UUID, BlockDisplay> getInteractionRefs() {
        return interactionRefs;
    }

    public void generateCannons() {
        NamespacedKey displayKey = new NamespacedKey(plugin, "display_uuid");
        for (Entity entity : entities) {
            if (entity instanceof Interaction interaction) {
                PersistentDataContainer container = interaction.getPersistentDataContainer();
                if (container.has(displayKey, PersistentDataType.STRING)) {
                    Entity refEntity = Bukkit.getEntity(UUID.fromString(container.get(displayKey, PersistentDataType.STRING)));
                    if (refEntity instanceof BlockDisplay blockDisplay && blockDisplay.getBlock().getMaterial() == Material.GRINDSTONE) {
                        addCannon(interaction);
                    }
                }
            }
        }
    }
}


