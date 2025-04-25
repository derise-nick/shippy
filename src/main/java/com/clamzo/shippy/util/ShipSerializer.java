package com.clamzo.shippy.util;

import com.clamzo.shippy.ShippyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

public class ShipSerializer {
    private final ShippyPlugin plugin;
    public ShipSerializer(ShippyPlugin plugin) {
        this.plugin = plugin;
    }

    public ActiveShip deserializeShip(SerializableActiveShip data) {
        World world = Bukkit.getWorld(data.standLocation.getWorld().getUID());
        if (world == null) return null;

        // ArmorStand
        ArmorStand stand = world.spawn(data.standLocation, ArmorStand.class, a -> {
            a.setInvisible(true);
            a.setGravity(true);
            a.setMarker(false);
            a.setPersistent(true);
            a.setInvulnerable(true);
            a.setCustomName("ShipController");
            a.setCustomNameVisible(false);
        });

        // BlockDisplays
        List<Entity> displays = new ArrayList<>();
        Map<UUID,BlockDisplay> cannons = new HashMap<>();
        for (var s : data.entities) {
            switch (s.entityType) {
                case EntityType.ITEM_DISPLAY:
                    ItemDisplay itemDisp = (ItemDisplay) world.spawnEntity(s.location, EntityType.ITEM_DISPLAY);
                    itemDisp.setItemStack(ItemStack.deserializeBytes(s.itemStack));
                    itemDisp.setTransformation(new Transformation(s.transformation, new AxisAngle4f(), new Vector3f(1, 1, 1), new AxisAngle4f()));
                    itemDisp.setPersistent(true);
                    itemDisp.setTeleportDuration(3);
                    displays.add(itemDisp);
                    break;
                case EntityType.BLOCK_DISPLAY:
                    BlockDisplay blockDisp = (BlockDisplay) world.spawnEntity(s.location, EntityType.BLOCK_DISPLAY);
                    blockDisp.setTransformation(new Transformation(s.transformation, new AxisAngle4f(), new Vector3f(1, 1, 1), new AxisAngle4f()));
                    blockDisp.setBlock(Bukkit.createBlockData(s.blockData));
                    blockDisp.setPersistent(true);
                    blockDisp.setTeleportDuration(3);
                    displays.add(blockDisp);
                    if (blockDisp.getBlock().getMaterial().equals(Material.DISPENSER)) cannons.put(blockDisp.getUniqueId(),blockDisp);
                    break;
                case EntityType.INTERACTION:
                    Interaction interaction = (Interaction) world.spawnEntity(s.location.clone().add(-0.5, 1.5, 0), EntityType.INTERACTION);
                    interaction.setInteractionHeight(1.5f);
                    interaction.setInteractionWidth(1.5f);
                    interaction.setInvulnerable(true);
                    interaction.setGravity(false);
                    interaction.setPersistent(true);
                    NamespacedKey helmKey = new NamespacedKey(plugin, "ship_armorstand_uuid");
                    interaction.getPersistentDataContainer().set(helmKey, PersistentDataType.STRING, stand.getUniqueId().toString());
                    interaction.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_helm"), PersistentDataType.BYTE, (byte) 1);
                    displays.add(interaction);
            }
        }

        return new ActiveShip(stand.getUniqueId(), stand, displays, cannons);
    }

    public SerializableActiveShip serializeShip(ActiveShip ship) {
        List<SerializableActiveShip.SerializedEntity> displays = ship.getEntities().stream().map(d -> {
            switch (d.getType()) {

                case EntityType.BLOCK_DISPLAY:
                    return new SerializableActiveShip.SerializedEntity(
                            d.getLocation(),
                            ((BlockDisplay) d).getTransformation().getTranslation(),
                            ((BlockDisplay) d).getBlock()
                    );
                case EntityType.ITEM_DISPLAY:
                    return new SerializableActiveShip.SerializedEntity(
                            d.getLocation(),
                            ((ItemDisplay) d).getItemStack(),
                            ((ItemDisplay) d).getTransformation().getTranslation()
                    );
                default:
                    return new SerializableActiveShip.SerializedEntity(
                        d.getLocation(),
                        d.getType()
                    );
            }
        }).collect(Collectors.toList());

        return new SerializableActiveShip(
                ship.getOwnerId(),
                ship.getStandEntity().getLocation(),
                displays
        );
    }
}
