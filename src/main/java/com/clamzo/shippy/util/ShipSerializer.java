package com.clamzo.shippy.util;

import com.clamzo.shippy.ShippyPlugin;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
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
        Chunk chunk = data.standLocation.getChunk();
        chunk.addPluginChunkTicket(plugin);
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
        List<Entity> entities = new ArrayList<>();
        Map<UUID,BlockDisplay> interactionRefs = new HashMap<>();
        for (var s : data.entities) {
            switch (s.entityType) {
                case EntityType.ITEM_DISPLAY:
                    ItemDisplay itemDisp = (ItemDisplay) world.spawnEntity(s.location, EntityType.ITEM_DISPLAY);
                    itemDisp.setItemStack(ItemStack.deserializeBytes(s.itemStack));
                    itemDisp.setTransformation(new Transformation(s.translation, s.rotation, new Vector3f(1.5f, 1.5f, 1.5f), new Quaternionf()));
                    itemDisp.setPersistent(true);
                    itemDisp.setTeleportDuration(3);
                    entities.add(itemDisp);
                    break;
                case EntityType.BLOCK_DISPLAY:
                    BlockDisplay blockDisp = (BlockDisplay) world.spawnEntity(s.location, EntityType.BLOCK_DISPLAY);
                    blockDisp.setTransformation(new Transformation(s.translation, new AxisAngle4f(), new Vector3f(1, 1, 1), new AxisAngle4f()));
                    blockDisp.setBlock(Bukkit.createBlockData(s.blockData));
                    blockDisp.setPersistent(true);
                    blockDisp.setTeleportDuration(3);
                    entities.add(blockDisp);
                    PersistentDataContainer pdc = blockDisp.getPersistentDataContainer();
                    boolean isCustomBlock = (!pdc.has(new NamespacedKey(plugin, "custom_blockpos"), PersistentDataType.STRING));
                    boolean isCannon = blockDisp.getBlock().getMaterial().equals(Material.GRINDSTONE) && isCustomBlock;
                    if (isCannon || blockDisp.getBlock().getMaterial().equals(Material.BARREL)) {
                        Interaction interaction = getInteraction(world, blockDisp, stand);
                        entities.add(interaction);
                        interactionRefs.put(blockDisp.getUniqueId(), blockDisp);
                    }
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
                    entities.add(interaction);
            }
        }
        ActiveShip newShip = new ActiveShip(stand.getUniqueId(), stand, entities, interactionRefs, data.helmHeight, plugin);
        newShip.generateCannons();
        chunk.removePluginChunkTicket(plugin);
        return newShip;
    }

    private @NotNull Interaction getInteraction(World world, BlockDisplay blockDisp, ArmorStand stand) {
        Interaction interaction = (Interaction) world.spawnEntity(blockDisp.getLocation(), EntityType.INTERACTION);
        interaction.setInteractionHeight(1f);
        interaction.setInteractionWidth(1f);
        interaction.setInvulnerable(true);
        interaction.setGravity(false);
        interaction.setPersistent(true);
        NamespacedKey dispId = new NamespacedKey(plugin, "display_uuid");
        NamespacedKey helmKey = new NamespacedKey(plugin, "ship_armorstand_uuid");
        interaction.getPersistentDataContainer().set(dispId, PersistentDataType.STRING, blockDisp.getUniqueId().toString());
        interaction.getPersistentDataContainer().set(helmKey, PersistentDataType.STRING, stand.getUniqueId().toString());
        return interaction;
    }

    public SerializableActiveShip serializeShip(ActiveShip ship) {
        List<SerializableActiveShip.SerializedEntity> displays = ship.getEntities().stream().map(d -> {
            return switch (d.getType()) {
                case EntityType.BLOCK_DISPLAY -> new SerializableActiveShip.SerializedEntity(
                        d.getLocation(),
                        ((BlockDisplay) d).getTransformation().getTranslation(),
                        ((BlockDisplay) d).getBlock()
                );
                case EntityType.ITEM_DISPLAY -> new SerializableActiveShip.SerializedEntity(
                        d.getLocation(),
                        ((ItemDisplay) d).getItemStack(),
                        ((ItemDisplay) d).getTransformation().getTranslation(),
                        ((ItemDisplay) d).getTransformation().getLeftRotation()
                );
                default -> new SerializableActiveShip.SerializedEntity(
                        d.getLocation(),
                        d.getType()
                );
            };
        }).collect(Collectors.toList());

        return new SerializableActiveShip(
                ship.getOwnerId(),
                ship.getStandEntity().getLocation(),
                displays,
                ship.getHelmHeight()
        );
    }
}
