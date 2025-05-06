package com.clamzo.shippy.behavior;

import com.clamzo.shippy.ShippyPlugin;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public class CustomBlockManager {
    private final Map<String, UUID> customItemDisplays;
    private final ShippyPlugin plugin;

    public CustomBlockManager(ShippyPlugin plugin) {
        this.plugin = plugin;
        this.customItemDisplays = new HashMap<>();
    }

    public void loadCustomBlockModels() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            NamespacedKey posKey = new NamespacedKey(plugin, "custom_blockpos");

            for (World world : Bukkit.getWorlds()) {
                for (ItemDisplay disp : world.getEntitiesByClass(ItemDisplay.class)) {
                    addItemDisplayToMap(disp, posKey, world);
                }
            }
        }, 20L);
    }

    public void addItemDisplayToMap(ItemDisplay disp, NamespacedKey posKey, World world) {
        PersistentDataContainer pdc = disp.getPersistentDataContainer();
        if (!pdc.has(posKey, PersistentDataType.STRING)) return;
        String[] parts = Objects.requireNonNull(pdc.get(posKey, PersistentDataType.STRING)).split(",");
        UUID worldId = UUID.fromString(parts[3]);
        if (!world.getUID().equals(worldId)) return;  // skip if world mismatch
        int bx = Integer.parseInt(parts[0]);
        int by = Integer.parseInt(parts[1]);
        int bz = Integer.parseInt(parts[2]);

        String key = worldId
                + ":" + bx
                + "," + by
                + "," + bz;
        customItemDisplays.put(key, disp.getUniqueId());
    }


    public String makeKey(Location loc) {
        return loc.getWorld().getUID()
                + ":" + loc.getBlockX()
                + "," + loc.getBlockY()
                + "," + loc.getBlockZ();
    }

    public Map<String, UUID> getCustomItemDisplays() {
        return customItemDisplays;
    }

    public void addCustomItemDisplay(@NotNull Location loc, ItemDisplay item) {
        customItemDisplays.put(makeKey(loc), item.getUniqueId());
    }

    public UUID removeCustomItemDisplay(Location loc) {
        return customItemDisplays.remove(makeKey(loc));
    }

    public boolean locationIsCustomBlock(Location loc) {
        String key = makeKey(loc);
        return customItemDisplays.containsKey(key);
    }

    public void spawnCannonDisplay(BlockDisplay display, List<Interaction> cannonList, Interaction interaction, List<Entity> entities) {
        cannonList.add(interaction);
        Location spawnLoc = display.getLocation().clone().add(0.5, 0.75, 0.5);
        Directional dir = (Directional) display.getBlock();
        BlockFace face = dir.getFacing();
        float spawnYaw = switch (face) {
            case NORTH -> 180f;
            case SOUTH -> 0f;
            case WEST -> -90f;
            case EAST -> 90f;
            default -> 0f;
        };
        spawnLoc.setRotation(spawnYaw, display.getPitch());
        ItemDisplay cannonView = (ItemDisplay) interaction.getWorld().spawnEntity(spawnLoc, EntityType.ITEM_DISPLAY);
        ItemStack cannon = new ItemStack(Material.GRINDSTONE, 1);
        ItemMeta cannonItemMeta = cannon.getItemMeta();
        cannonItemMeta.setCustomModelData(313);
        cannon.setItemMeta(cannonItemMeta);
        cannonView.setItemStack(cannon);

        cannonView.setPersistent(true);
        cannonView.setTeleportDuration(3);

        float rad = (float) Math.toRadians(spawnYaw);
        AxisAngle4f yawRot = new AxisAngle4f(rad, 0, 1, 0);
        Transformation cannonTrans = new Transformation(
                display.getTransformation().getTranslation().add(0.5f,0.75f,0.5f),
                yawRot,
                new Vector3f(1.5f, 1.5f, 1.5f),
                new AxisAngle4f(0, 0, 0, 0)
        );
        cannonView.setTransformation(cannonTrans);
        entities.add(cannonView);
    }
}
