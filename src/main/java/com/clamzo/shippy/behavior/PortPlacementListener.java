package com.clamzo.shippy.behavior;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.util.PortAndShipManager;
import com.clamzo.shippy.util.SavedBlock;
import net.kyori.adventure.text.Component;
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
import org.bukkit.util.Vector;

public class PortPlacementListener implements Listener {
    private final int portWidth = 8;
    private final int portLength = 12;
    private final int portHeight = 8;

    // TODO: Make the port work with whoever hit the button
    // TODO: Fix the facing of the ports when they aren't facing south
    // TODO: Trigger from sign use instead of button

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

        for (int x = 0; x < portWidth; x++) {
            for (int z = 0; z < portLength; z++) {
                for (int y = 0; y < portHeight; y++) {
                    Location check = offsetByFacing(base, x, y, z, facing);
                    Material type = world.getBlockAt(check).getType();
                    if (!type.isAir() || type.isCollidable()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    private void placePortStructure(Location base) {
        World world = base.getWorld();

        for (int x = 0; x < portWidth; x++) {
            for (int z = 0; z < portLength; z++) {
                for (int y = 0; y < portHeight; y++) {
                    Location loc = offsetByFacing(base, x, y, z, facing);
                    Material mat;

                    // Floor layer = Stone Bricks
                    if (y == 0) {
                        mat = Material.STONE_BRICKS;
                    }
                    // Pillars at corners
                    else if ((x == 0 || x == portWidth - 1) && (z == 0 || z == portLength - 1)) {
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

//    @EventHandler
//    public void onRedstoneChange(BlockRedstoneEvent event) {
//        // TODO: Check on changing event to right-click sign
//        if (event.getNewCurrent() == 0) return;
//        Block block = event.getBlock();
//        if (block.getType() != Material.STONE_BUTTON) return;
//
//        Map<UUID, Location> portLocs = this.manager.getPortLocations();
//
//        Location loc = block.getLocation().clone().add(0,-1,0);
//
//        if (portLocs.containsValue(loc)) {
//            UUID owner = portLocs.entrySet().stream()
//                    .filter(entry -> entry.getValue().equals(loc))
//                    .map(Map.Entry::getKey)
//                    .findFirst()
//                    .orElse(null);
//            for (World wor:Bukkit.getWorlds()) {
//                for (Player p : wor.getPlayers()) {
//                    p.sendMessage(loc.toString());
//                }
//            }
//            if (owner != null && Bukkit.getPlayer(owner) != null) {
//                Player boatOwner = Bukkit.getPlayer(owner);
//                boatOwner.sendMessage(NamedTextColor.AQUA + "Ship saved!");
//                Location base = manager.getPortLocations().get(owner);
//                if (base == null) return;
//
//                List<SavedBlock> shipBlocks = new ArrayList<>();
//
//                int helmCount = 0;
//                SavedBlock helmBlock = null;
//
//                for (int x = 0; x < portWidth; x++) {
//                    for (int y = 1; y < portHeight; y++) {
//                        for (int z = 0; z < portLength; z++) {
//                            // Skip corner blocks
//                            boolean isCorner =
//                                    (x == 0 || x == portWidth - 1) &&
//                                            (z == 0 || z == portLength - 1);
//                            if (isCorner) continue;
//
//                            Location saveLoc = offsetByFacing(base, x, y, z, getCardinalFacing(boatOwner));
//                            Material type = saveLoc.getBlock().getType();
//
//                            if (type == Material.LODESTONE) {
//                                helmCount++;
//                            }
//
//                            if (type != Material.AIR) {
//                                SavedBlock sb = new SavedBlock(x-portWidth/2, y, z-portLength/2, Bukkit.createBlockData(type));
//                                shipBlocks.add(sb);
//                            }
//                        }
//                    }
//                }
//
//                if (helmCount == 0) {
//                    boatOwner.sendMessage(Component.text("Your ship must have exactly one LODESTONE block to act as a helm.").color(NamedTextColor.RED));
//                    return;
//                } else if (helmCount > 1) {
//                    boatOwner.sendMessage(Component.text("Your ship has multiple helm blocks. Please leave only one LODESTONE.").color(NamedTextColor.RED));
//                    return;
//                }
//
//                manager.addShipForUser(owner, shipBlocks);
//
//                ItemStack shipItem = new ItemStack(Material.OAK_CHEST_BOAT);
//                ItemMeta meta = shipItem.getItemMeta();
//                meta.setDisplayName("Your Custom Ship");
//
//                meta.getPersistentDataContainer().set(this.key, PersistentDataType.STRING, owner.toString() + "_ship");
//
//                shipItem.setItemMeta(meta);
//                boatOwner.getInventory().addItem(shipItem);
//            }
//        }
//    }
    @EventHandler
    public void onRedstoneChange(BlockRedstoneEvent event) {
        // TODO: Check on changing event to right-click sign
        if (event.getNewCurrent() == 0) return;
        Block button = event.getBlock();
        if (button.getType() != Material.STONE_BUTTON) return;

        Map<UUID, Location> portLocs = this.manager.getPortLocations();

        Location loc = button.getLocation().clone().add(0,-1,0);

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

                Location helmLoc = null;
                int helmCount = 0;

                // First pass: Find helm location
                for (int x = 0; x < portWidth; x++) {
                    for (int y = 1; y < portHeight; y++) {
                        for (int z = 0; z < portLength; z++) {
                            boolean isCorner =
                                    (x == 0 || x == portWidth - 1) &&
                                            (z == 0 || z == portLength - 1);
                            if (isCorner) continue;

                            Location testLoc = offsetByFacing(base, x, y, z, getCardinalFacing(boatOwner));
                            if (testLoc.getBlock().getType() == Material.LODESTONE) {
                                helmLoc = testLoc;
                                helmCount++;
                            }
                        }
                    }
                }

                if (helmCount == 0) {
                    boatOwner.sendMessage(Component.text("Your ship must have exactly one LODESTONE block to act as a helm.").color(NamedTextColor.RED));
                    return;
                } else if (helmCount > 1) {
                    boatOwner.sendMessage(Component.text("Your ship has multiple helm blocks. Please leave only one LODESTONE.").color(NamedTextColor.RED));
                    return;
                }

                Vector helmVector = helmLoc.toVector(); // Used for relative offsets

                for (int x = 0; x < portWidth; x++) {
                    for (int y = 1; y < portHeight; y++) {
                        for (int z = 0; z < portLength; z++) {
                            boolean isCorner =
                                    (x == 0 || x == portWidth - 1) &&
                                            (z == 0 || z == portLength - 1);
                            if (isCorner) continue;

                            Location saveLoc = offsetByFacing(base, x, y, z, getCardinalFacing(boatOwner));
                            Block block = saveLoc.getBlock();
                            BlockData bd = block.getBlockData().clone();
                            if (bd.getMaterial() == Material.AIR || bd.getMaterial() == Material.LODESTONE) continue; // Skip empty and helm

                            Vector rel = saveLoc.toVector().subtract(helmVector);
                            SavedBlock sb = new SavedBlock(rel.getBlockX(), rel.getBlockY(), rel.getBlockZ(), bd);
                            shipBlocks.add(sb);
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
