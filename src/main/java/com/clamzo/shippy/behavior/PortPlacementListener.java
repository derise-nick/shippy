package com.clamzo.shippy.behavior;

import java.util.*;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.util.PortAndShipManager;
import com.clamzo.shippy.util.SavedBlock;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class PortPlacementListener implements Listener {

    // TODO: Make the port work with whoever hit the button
    // TODO: Fix the facing of the ports when they aren't facing north
    // TODO: Don't save the bricks and logs
    // TODO: Find a good, consistent button space on port spawn

    private BlockFace facing = BlockFace.NORTH;
    private final PortAndShipManager manager;
    private final NamespacedKey key;

    public PortPlacementListener(ShippyPlugin plugin) {
        this.manager = plugin.manager;
        key = new NamespacedKey(plugin, "ship_id");
    }

    @EventHandler
    public void onPortItemUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BLAZE_POWDER) return;

        // Check item name
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        if (!meta.getDisplayName().equalsIgnoreCase("Port")) return;

        // Cancel the default interaction
        event.setCancelled(true);

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        facing = getCardinalFacing(player);

        // Get the base location (one block above clicked surface)
        Location baseLocation = clickedBlock.getRelative(facing).getLocation();

        if (!hasRoomForPort(baseLocation)) {
            player.sendMessage(NamedTextColor.RED + "Not enough space to place the Port.");
            return;
        }

        placePortStructure(baseLocation);
        savePortLocation(player.getUniqueId(), baseLocation);
        player.sendMessage(NamedTextColor.GREEN + "Port placed successfully!");
    }

    private boolean hasRoomForPort(Location base) {
        World world = base.getWorld();

        int width = 4;
        int length = 6;
        int height = 5;

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                for (int y = 0; y < height; y++) {
                    Location check = offsetByFacing(base, x, y, z, facing);
                    Material type = world.getBlockAt(check).getType();
                    if (!type.isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    private void placePortStructure(Location base) {
        // TODO - set this thing down in whatever direction the player is facing, instead of directly north or whatever
        World world = base.getWorld();

        int width = 6;
        int length = 8;
        int height = 8;

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                for (int y = 0; y < height; y++) {
                    Location loc = offsetByFacing(base, x, y, z, facing);
                    Material mat;

                    // Floor layer = Stone Bricks
                    if (y == 0) {
                        mat = Material.STONE_BRICKS;
                    }
                    // Pillars at corners
                    else if ((x == 0 || x == width - 1) && (z == 0 || z == length - 1)) {
                        mat = Material.OAK_LOG;
                    }
                    // Air gaps
                    else {
                        mat = Material.AIR;
                    }

                    world.getBlockAt(loc).setType(mat);
                }
            }
        }

        // Place controller block (used for redstone)
        Location buttonLoc = offsetByFacing(base, 0, 1, 0, facing);
        world.getBlockAt(buttonLoc).setType(Material.STONE_BUTTON);
    }

    private BlockFace getCardinalFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        yaw = (yaw % 360 + 360) % 360; // Normalize
        if (yaw < 45 || yaw >= 315) return BlockFace.SOUTH;
        if (yaw < 135) return BlockFace.WEST;
        if (yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }

    private Location offsetByFacing(Location origin, int dx, int dy, int dz, BlockFace direction) {
        switch (direction) {
            case NORTH: return origin.clone().add(-dx, dy, -dz);
            case SOUTH: return origin.clone().add(dx, dy, dz);
            case WEST:  return origin.clone().add(-dz, dy, dx);
            case EAST:  return origin.clone().add(dz, dy, -dx);
            default: return origin.clone().add(dx, dy, dz); // fallback
        }
    }

    @EventHandler
    public void onRedstoneChange(BlockRedstoneEvent event) {
        // TODO: Check on changing event to right-click sign
        if (event.getNewCurrent() == 0) return;
        Block block = event.getBlock();
        if (block.getType() != Material.STONE_BUTTON) return;

        Map<UUID, Location> portLocs = this.manager.getPortLocations();

        Location loc = block.getLocation().clone().add(0,-1,0);

        if (portLocs.containsValue(loc)) {
            UUID owner = portLocs.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(loc))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            for (World wor:Bukkit.getWorlds()) {
                for (Player p : wor.getPlayers()) {
                    p.sendMessage(loc.toString());
                }
            }
            if (owner != null && Bukkit.getPlayer(owner) != null) {
                Player boatOwner = Bukkit.getPlayer(owner);
                boatOwner.sendMessage(NamedTextColor.AQUA + "Ship saved!");
                Location base = manager.getPortLocations().get(owner);
                if (base == null) return;

                List<SavedBlock> shipBlocks = new ArrayList<>();

                int width = 6;
                int length = 8;
                int height = 8;

                for (int x = 0; x < width; x++) {
                    for (int y = 1; y < height; y++) {
                        for (int z = 0; z < length; z++) {
                            // Skip corner blocks
                            boolean isCorner =
                                    (x == 0 || x == width - 1) &&
                                            (z == 0 || z == length - 1);
                            if (isCorner) continue;

                            Location saveLoc = offsetByFacing(base, x, y, z, getCardinalFacing(boatOwner));
                            Material type = saveLoc.getBlock().getType();

                            if (type != Material.AIR) {
                                SavedBlock sb = new SavedBlock(x-width/2, y, z-length/2, Bukkit.createBlockData(type));
                                shipBlocks.add(sb);
                            }
                        }
                    }
                }


                manager.addShipForUser(owner, shipBlocks);

                ItemStack shipItem = new ItemStack(Material.OAK_CHEST_BOAT);
                ItemMeta meta = shipItem.getItemMeta();
                meta.setDisplayName("Your Custom Ship");


                meta.getPersistentDataContainer().set(this.key, PersistentDataType.STRING, owner.toString() + "_ship");

                shipItem.setItemMeta(meta);
                boatOwner.getInventory().addItem(shipItem);
            }
        }
    }


    private void savePortLocation(UUID playerId, Location loc) {
        this.manager.addPortLocation(playerId, loc);
    }

}
