package com.clamzo.shippy.behavior;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.util.PortAndShipManager;
import com.clamzo.shippy.util.SavedBlock;
import com.clamzo.shippy.util.StructurePlacementUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShipyardListener implements Listener {
    private final int shipyardWidth = 8;
    private final int shipyardLength = 12;
    private final int shipyardHeight = 8;

    // TODO: Make the port work with whoever hit the button
    // TODO: Fix the buttons to work facing-agnostically
    // TODO: Trigger from sign use instead of button

    private BlockFace facing = BlockFace.NORTH;
    private final PortAndShipManager manager;
    private final NamespacedKey key;
    private final ShippyPlugin plugin;

    public ShipyardListener(ShippyPlugin plugin) {
        this.manager = plugin.manager;
        this.plugin = plugin;
        key = new NamespacedKey(plugin, "ship_id");
    }

    @EventHandler
    public void onShipyardItemUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BLAZE_POWDER) return;

        // Check item name
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        if (!meta.getDisplayName().equalsIgnoreCase("Shipyard")) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        facing = StructurePlacementUtil.getCardinalFacingForPlayer(player);

        // Get the base location (one block above clicked surface)
        Location baseLocation = clickedBlock.getRelative(facing).getLocation();

        if (!hasRoomForShipyard(baseLocation)) {
            player.sendMessage(Component.text("Not enough space to place the Shipyard.").color(NamedTextColor.RED));
            return;
        }
        StructurePlacementUtil.placeStructure(baseLocation, shipyardWidth, shipyardLength, shipyardHeight, facing);
        String facing = StructurePlacementUtil.getCardinalFacingForPlayer(player).toString();
        this.manager.addShipyardLocation(player.getUniqueId(), baseLocation.setRotation(StructurePlacementUtil.getYawFromFacing(facing), 0));
        player.sendMessage(Component.text("Shipyard placed successfully!").color(NamedTextColor.GREEN));
    }

    private boolean hasRoomForShipyard(Location base) {
        // TODO: Move this method to util
        World world = base.getWorld();

        for (int x = 0; x < shipyardWidth; x++) {
            for (int z = 0; z < shipyardLength; z++) {
                for (int y = 0; y < shipyardHeight; y++) {
                    Location check = StructurePlacementUtil.offsetByFacing(base, x, y, z, facing);
                    Material type = world.getBlockAt(check).getType();
                    if (!type.isAir() || type.isCollidable()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @EventHandler
    public void onRedstoneChange(BlockRedstoneEvent event) {
        if (event.getNewCurrent() == 0) return;
        Block button = event.getBlock();
        if (button.getType() != Material.STONE_BUTTON) return;

        Location loc = button.getLocation().clone().add(0,-1,0);
        plugin.getLogger().info("Finding port for location: " + loc);
        Location shipyard = manager.findShipyardForLocation(loc);
        if (shipyard != null) {
            plugin.getLogger().info("Saving ship at port location: " + loc);
            Player nearestPlayer = button.getWorld().getNearbyEntities(loc, 3, 3, 3).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .findFirst()
                    .orElse(null);
            if (nearestPlayer != null) {
                nearestPlayer.sendMessage(Component.text("Ship saved!").color(NamedTextColor.AQUA));
                List<SavedBlock> shipBlocks = new ArrayList<>();
                Location helmLoc = null;
                int helmCount = 0;

                // First pass: Find helm location
                for (int x = 0; x < shipyardWidth; x++) {
                    for (int y = 1; y < shipyardHeight; y++) {
                        for (int z = 0; z < shipyardLength; z++) {
                            boolean isCorner =
                                    (x == 0 || x == shipyardWidth - 1) &&
                                            (z == 0 || z == shipyardLength - 1);
                            if (isCorner) continue;

                            Location testLoc = StructurePlacementUtil.offsetByFacing(shipyard, x, y, z, StructurePlacementUtil.getCardinalFacing(shipyard));
                            if (testLoc.getBlock().getType() == Material.LODESTONE) {
                                helmLoc = testLoc;
                                helmCount++;
                            }
                        }
                    }
                }

                if (helmCount == 0) {
                    nearestPlayer.sendMessage(Component.text("Your ship must have exactly one LODESTONE block to act as a helm.").color(NamedTextColor.RED));
                    return;
                } else if (helmCount > 1) {
                    nearestPlayer.sendMessage(Component.text("Your ship has multiple helm blocks. Please leave only one LODESTONE.").color(NamedTextColor.RED));
                    return;
                }

                Vector helmVector = helmLoc.toVector(); // Used for relative offsets

                for (int x = 0; x < shipyardWidth; x++) {
                    for (int y = 1; y < shipyardHeight; y++) {
                        for (int z = 0; z < shipyardLength; z++) {
                            boolean isCorner =
                                    (x == 0 || x == shipyardWidth - 1) &&
                                            (z == 0 || z == shipyardLength - 1);
                            if (isCorner) continue;

                            Location saveLoc = StructurePlacementUtil.offsetByFacing(shipyard, x, y, z, StructurePlacementUtil.getCardinalFacing(shipyard));
                            Block block = saveLoc.getBlock();
                            BlockData bd = block.getBlockData().clone();
                            if (bd.getMaterial() == Material.AIR || bd.getMaterial() == Material.LODESTONE) continue; // Skip empty and helm

                            Vector rel = saveLoc.toVector().subtract(helmVector);
                            SavedBlock sb = new SavedBlock(rel.getBlockX(), rel.getBlockY(), rel.getBlockZ(), bd);
                            shipBlocks.add(sb);
                        }
                    }
                }

                UUID shipId = UUID.randomUUID();
                // Preventing duplicates
                while (!this.manager.getShipStructure(shipId).isEmpty()) {
                    shipId = UUID.randomUUID();
                }
                manager.addShipForUser(shipId, shipBlocks, nearestPlayer);

                ItemStack shipItem = new ItemStack(Material.OAK_CHEST_BOAT);
                ItemMeta meta = shipItem.getItemMeta();
                meta.setDisplayName("Your Custom Ship");

                meta.getPersistentDataContainer().set(this.key, PersistentDataType.STRING,  shipId.toString());

                shipItem.setItemMeta(meta);
                nearestPlayer.getInventory().addItem(shipItem);
            }
        }
    }

}
