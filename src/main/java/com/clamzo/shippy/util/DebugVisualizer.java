package com.clamzo.shippy.util;

import com.clamzo.shippy.ship.ActiveShip;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                        List<OrientedBoundingBox> obbs = ship.getBoundingBoxes();
                        for (OrientedBoundingBox obb : obbs) {
                            drawOBB(player.getWorld(), obb);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
    private static final double PARTICLE_STEP = 0.25;

    private void drawOBB(World world, OrientedBoundingBox obb) {
        List<Vector> corners = obb.getCorners();
        // 12 edges: each pair of corner indices
        int[][] edges = {
                {0,1},{0,2},{0,4},
                {7,5},{7,6},{7,3},
                {1,3},{1,5},
                {2,3},{2,6},
                {4,5},{4,6}
        };

        for (int[] edge : edges) {
            Vector a = corners.get(edge[0]);
            Vector b = corners.get(edge[1]);
            drawLine(world, a, b);
        }
    }

    /** Draw a particle line between two points */
    private void drawLine(World world, Vector start, Vector end) {
        Vector dir = end.clone().subtract(start);
        double length = dir.length();
        dir.normalize();
        for (double d = 0; d <= length; d += PARTICLE_STEP) {
            Vector point = start.clone().add(dir.clone().multiply(d));
            world.spawnParticle(Particle.DUST,
                    point.getX(), point.getY(), point.getZ(),
                    1, 0, 0, 0,
                    new Particle.DustOptions(Color.YELLOW, 1));
        }
    }

}
