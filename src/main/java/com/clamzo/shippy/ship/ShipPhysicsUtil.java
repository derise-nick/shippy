package com.clamzo.shippy.ship;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.util.OrientedBoundingBox;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class ShipPhysicsUtil {
    /** How far each BlockDisplay “reaches” from its center when blocking movement. */
    private static final double DISPLAY_PADDING = 1.1;
    private final ShippyPlugin plugin;

    public ShipPhysicsUtil(ShippyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Return true if moving the ship anchor to `futureAnchor` would *not* collide
     * any solid blocks in the world.
     */
    public static boolean canMoveTo(Location futureAnchor, ActiveShip ship, World world) {
        List<OrientedBoundingBox> obbs =
                ship.calculateOBBs(futureAnchor.getYaw(), futureAnchor);

        for (OrientedBoundingBox obb : obbs) {
            // Test only the 8 corners of each box
            for (Vector corner : obb.getCorners()) {
                int bx = corner.getBlockX();
                int by = corner.getBlockY();
                int bz = corner.getBlockZ();

                Block block = world.getBlockAt(bx, by, bz);
                if (!block.isPassable() && block.getType().isSolid()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void activateDeckPhysics() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ActiveShip ship = plugin.getShipManager().getDeckForPlayer(player);
                if (ship == null) {
                    if (!player.hasGravity()) player.setGravity(true);
                    continue;
                }

                if (player.hasGravity()) {
                    player.setGravity(false);
                    player.setFallDistance(0);
                }

                Input in = player.getCurrentInput();
                float yaw = player.getLocation().getYaw();
                Vector forward = new Vector(
                        -Math.sin(Math.toRadians(yaw)),
                        0,
                        Math.cos(Math.toRadians(yaw))
                ).normalize();
                Vector left = forward.clone().crossProduct(new Vector(0,1,0)).normalize();

                Vector motion = new Vector(0, 0, 0);
                if (in.isForward())  motion.add(forward);
                if (in.isBackward()) motion.subtract(forward);
                if (in.isLeft())     motion.subtract(left);
                if (in.isRight())    motion.add(left);
                if (motion.lengthSquared() > 0) {
                    motion.normalize().multiply(0.15);
                }

                // Jump handling
                if (in.isJump()) {
                    motion.setY(0.42);
                } else {
                    motion.setY(0);
                }

                motion.add(ship.getStandEntity().getVelocity());

                // Calculate angular velocity of rotating ship and move accordingly
                double omega = ship.getController().getAngularVelocity();
                if (omega != 0) {
                    Vector offset = player.getLocation().toVector()
                            .subtract(ship.getStandEntity().getLocation().toVector());
                    Vector tangential = new Vector(
                            offset.getZ() * omega,
                            0,
                            -offset.getX() * omega
                    );
                    motion.add(tangential);
                }

                player.setVelocity(motion);
            }
        }, 0L, 1L);

    }
}
