package com.clamzo.shippy.structures;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.util.StructurePlacementUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class StructurePreviewListener implements Listener {
    private final Map<UUID, List<BlockDisplay>> activePreviews = new HashMap<>();
    private final ShippyPlugin plugin;

    public StructurePreviewListener(ShippyPlugin plugin) {
        this.plugin = plugin;
    }

    public void showPreview(Player player, Location base, int width, int length, int height, BlockFace facing) {
        // First clear any old preview
        clearPreview(player);

        List<BlockDisplay> displays = new ArrayList<>();
        World world = player.getWorld();

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                for (int y = 0; y < height; y++) {
                    Location loc = StructurePlacementUtil.offsetByFacing(base, x, y, z, facing);

                    Material mat;
                    if (y == 0) {
                        mat = Material.STONE_BRICKS;
                    } else if ((x == 0 || x == width - 1) && (z == 0 || z == length - 1)) {
                        mat = Material.OAK_LOG;
                    } else {
                        mat = Material.AIR;
                    }

                    if (mat != Material.AIR) {
                        BlockDisplay bd = (BlockDisplay) world.spawnEntity(loc, EntityType.BLOCK_DISPLAY);
                        bd.setBlock(Bukkit.createBlockData(mat));

                        // Make it translucent and glowy
                        Display.Brightness brightness = new Display.Brightness(4, 4);
                        bd.setBrightness(brightness);

                        displays.add(bd);
                    }
                }
            }
        }

        activePreviews.put(player.getUniqueId(), displays);
    }

    public void clearPreview(Player player) {
        List<BlockDisplay> displays = activePreviews.remove(player.getUniqueId());
        if (displays != null) {
            for (BlockDisplay bd : displays) {
                bd.remove();
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        clearPreview(player);// or offhand if you want
        if (item == null || item.getType() != Material.BLAZE_POWDER) {
            return;
        }

        Location loc = player.getLocation();
        BlockFace facing = StructurePlacementUtil.getCardinalFacing(loc);

        if (event.isSneaking()) {
            // Start preview
            showPreview(player, loc.getBlock().getLocation().clone().add(facing.getModX(), facing.getModY() - 1, facing.getModZ()), 8, 12, 8, facing);
        }
    }

}
