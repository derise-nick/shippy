package com.clamzo.shippy.util;

import com.clamzo.shippy.ShippyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

//    public static boolean canMoveTo(Location futureAnchor, ActiveShip ship, World world) {
//
//        List<OrientedBoundingBox> boxes = ship.calculateOBBs(futureAnchor.getYaw(), futureAnchor);
//
//        // 2) test each OBB against the world
//        for (OrientedBoundingBox box : boxes) {
//            int minX = (int) Math.floor(box.getMinX());
//            int maxX = (int) Math.ceil (box.getMaxX());
//            int minY = (int) Math.floor(box.getMinY());
//            int maxY = (int) Math.ceil (box.getMaxY());
//            int minZ = (int) Math.floor(box.getMinZ());
//            int maxZ = (int) Math.ceil (box.getMaxZ());
//
//            for (int x = minX; x <= maxX; x++) {
//                for (int y = minY; y <= maxY; y++) {
//                    for (int z = minZ; z <= maxZ; z++) {
//                        Block block = world.getBlockAt(x, y, z);
//                        if (!block.isPassable() && block.getType().isSolid()) {
//                            return false;
//                        }
//                    }
//                }
//            }
//        }
//        return true;
//    }

    /**
     * Compute an axis‐aligned bounding box for each Display entity,
     * given the ship’s anchor point in world coordinates.
     * These boxes can be used both for collision checks (above)
     * and for “on‐deck” foot detection.
     */
//    @NotNull
//    public static List<BoundingBox> computeBoundingBoxes(@NotNull Location anchor, List<Entity> entities) {
//        List<BoundingBox> boxes = new ArrayList<>(entities.size());
//
//        for (Entity e : entities) {
//            if (!(e instanceof Display d)) continue;
//
//            // 1) get the local translation (in blocks) from the Display's transformation
//            Vector3f t = d.getTransformation().getTranslation();
//            Vector offset = new Vector(t.x(), t.y(), t.z());
//
//            // 2) compute the world‐space center of this display
//            Vector center = anchor.toVector().add(offset);
//
//            // 3) make a 1×1×1 box around that center, then inflate by PADDING
//            BoundingBox box = BoundingBox.of(center, DISPLAY_PADDING,DISPLAY_PADDING,DISPLAY_PADDING);
//            boxes.add(box);
//        }
//
//        return boxes;
//    }
    public void activateDeckPhysics() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ActiveShip ship = plugin.getManager().getDeckForPlayer(player);
                if (ship == null) {
                    // Not on deck → restore normal physics
                    if (!player.hasGravity()) player.setGravity(true);
                    continue;
                }

                // On deck → disable gravity
                if (player.hasGravity()) {
                    player.setGravity(false);
                    player.setFallDistance(0);
                }

                // Read the input
                Input in = player.getCurrentInput();
                float yaw = player.getLocation().getYaw();
                Vector forward = new Vector(
                        -Math.sin(Math.toRadians(yaw)),
                        0,
                        Math.cos(Math.toRadians(yaw))
                ).normalize();
                Vector left = forward.clone().crossProduct(new Vector(0,1,0)).normalize();

                // Build motion vector
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
                    motion.setY(0.42);           // vanilla jump
                } else {
                    motion.setY(0);              // no gravity → stick to deck
                }

                motion.add(ship.getStandEntity().getVelocity());
                // ——— ship turning “centrifugal” push ———

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
