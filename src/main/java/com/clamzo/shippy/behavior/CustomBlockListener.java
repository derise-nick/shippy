package com.clamzo.shippy.behavior;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.util.StructurePlacementUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CustomBlockListener implements Listener {
    private final ShippyPlugin plugin;
    private final Map<Block, ItemDisplay> customBlocks;
    private final Map<BlockPosKey, UUID> customItemDisplays;

    public CustomBlockListener(ShippyPlugin plugin) {
        this.plugin = plugin;
        this.customBlocks = new HashMap<>();
        this.customItemDisplays = new HashMap<>();
    }
    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() != 0) {
            Block block = event.getBlockPlaced();
            BlockFace facing = StructurePlacementUtil.getCardinalFacingForPlayer(event.getPlayer());
            Location spawnLoc = block.getLocation().clone().add(0.5, 0.75, 0.5);
            spawnLoc.setRotation(StructurePlacementUtil.getYawFromFacing(facing.toString()), spawnLoc.getPitch());
            ItemDisplay display = (ItemDisplay) event.getPlayer().getWorld().spawnEntity(
                    spawnLoc, EntityType.ITEM_DISPLAY);
            display.setItemStack(item);
            display.setPersistent(true);
            BlockPosKey bpk = getBpkFromLoc(block.getLocation());
            customItemDisplays.put(bpk, display.getUniqueId());

            display.setTransformation(new Transformation(
                    new Vector3f(0,0,0),
                    new AxisAngle4f(0, 0, 0, 0),
                    new Vector3f(1.5f, 1.5f, 1.5f),
                    new AxisAngle4f(0, 0, 0, 0)
            ));

            NamespacedKey posKey = new NamespacedKey(plugin, "custom_blockpos");
            String posString = block.getX() + ","
                    + block.getY() + ","
                    + block.getZ() + ","
                    + block.getWorld().getUID().toString();
            display.getPersistentDataContainer().set(posKey, PersistentDataType.STRING, posString);

        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        assert event.getClickedBlock() != null;
        BlockPosKey bpk = getBpkFromLoc(event.getClickedBlock().getLocation());
         if (!customItemDisplays.containsKey(bpk)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        BlockPosKey key = getBpkFromLoc(e.getBlock().getLocation());
        UUID displayId = customItemDisplays.remove(key);
        if (displayId != null) {
            Objects.requireNonNull(Bukkit.getEntity(displayId)).remove();
        }
    }

    public record BlockPosKey(UUID worldUuid, int x, int y, int z) {}
    public BlockPosKey getBpkFromLoc(Location blockLoc) {
        return new BlockPosKey(
                blockLoc.getWorld().getUID(),
                blockLoc.getBlockX(),
                blockLoc.getBlockY(),
                blockLoc.getBlockZ()
        );
    }

    @EventHandler
    public void onEntitiesLoaded(EntitiesLoadEvent event) {
        NamespacedKey posKey = new NamespacedKey(plugin, "custom_blockpos");
        for (Entity e : event.getEntities()) {
            if (e instanceof ItemDisplay itemDisp) {
                addItemDisplayToMap(itemDisp, posKey, event.getWorld());
            }
        }
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

    private void addItemDisplayToMap(ItemDisplay disp, NamespacedKey posKey, World world) {
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
}
