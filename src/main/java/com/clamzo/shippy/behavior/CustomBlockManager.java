package com.clamzo.shippy.behavior;

import com.clamzo.shippy.ShippyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CustomBlockManager {
    private final Map<BlockPosKey, UUID> customItemDisplays;
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
        var pdc = disp.getPersistentDataContainer();
        if (!pdc.has(posKey, PersistentDataType.STRING)) return;
        String[] parts = Objects.requireNonNull(pdc.get(posKey, PersistentDataType.STRING)).split(",");
        UUID worldId = UUID.fromString(parts[3]);
        if (!world.getUID().equals(worldId)) return;  // skip if world mismatch
        int bx = Integer.parseInt(parts[0]);
        int by = Integer.parseInt(parts[1]);
        int bz = Integer.parseInt(parts[2]);

        BlockPosKey key = new BlockPosKey(worldId, bx, by, bz);
        customItemDisplays.put(key, disp.getUniqueId());
    }


    public BlockPosKey getBpkFromLoc(Location blockLoc) {
        return new BlockPosKey(
                blockLoc.getWorld().getUID(),
                blockLoc.getBlockX(),
                blockLoc.getBlockY(),
                blockLoc.getBlockZ()
        );
    }

    public Map<BlockPosKey, UUID> getCustomItemDisplays() {
        return customItemDisplays;
    }

    public void addCustomItemDisplay(BlockPosKey bpk, ItemDisplay item) {
        customItemDisplays.put(bpk, item.getUniqueId());
    }

    public UUID removeCustomItemDisplay(BlockPosKey bpk) {
        return customItemDisplays.remove(bpk);
    }

    public boolean locationIsCustomBlock(Location loc) {
        BlockPosKey bpk = getBpkFromLoc(loc);
        return customItemDisplays.containsKey(bpk);
    }
}
