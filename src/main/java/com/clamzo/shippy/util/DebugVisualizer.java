package com.clamzo.shippy.util;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.*;

public class DebugVisualizer {

    private final JavaPlugin plugin;
    private final Map<UUID, ActiveShip> debugTargets = new HashMap<>();

    public DebugVisualizer(JavaPlugin plugin) {
        this.plugin = plugin;
        startDebugTask();
    }

    public void enableDebugFor(Player player, ActiveShip ship) {
        debugTargets.put(player.getUniqueId(), ship);
    }

    public void disableDebugFor(Player player) {
        debugTargets.remove(player.getUniqueId());
    }

    public boolean isDebugging(Player player) {
        return debugTargets.containsKey(player.getUniqueId());
    }

    private void startDebugTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isSneaking() && isDebugging(player)) {
                        ActiveShip ship = debugTargets.get(player.getUniqueId());
                        if (ship != null) {
                            for (BoundingBox box : ship.getBoundingBoxes()) {
                                drawBoundingBox(player.getWorld(), box);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void drawBoundingBox(World world, BoundingBox box) {
        double step = 0.4;
        for (double x = box.getMinX(); x <= box.getMaxX(); x += step) {
            for (double y = box.getMinY(); y <= box.getMaxY(); y += step) {
                for (double z = box.getMinZ(); z <= box.getMaxZ(); z += step) {
                    int edgeCount = 0;
                    if (x == box.getMinX() || x == box.getMaxX()) edgeCount++;
                    if (y == box.getMinY() || y == box.getMaxY()) edgeCount++;
                    if (z == box.getMinZ() || z == box.getMaxZ()) edgeCount++;
                    if (edgeCount >= 2) {
                        world.spawnParticle(Particle.DUST,
                                x, y, z, 1,
                                new Particle.DustOptions(Color.AQUA, 1));
                    }
                }
            }
        }
    }
}
