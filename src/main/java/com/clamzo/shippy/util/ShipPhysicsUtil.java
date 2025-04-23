package com.clamzo.shippy.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ShipPhysicsUtil {
    /** How far each BlockDisplay “reaches” from its center when blocking movement. */
    private static final double DISPLAY_PADDING = 1.1;

    /**
     * Return true if moving the ship anchor to `futureAnchor` would *not* collide
     * any solid blocks in the world.
     */
    public static boolean canMoveTo(Location futureAnchor, List<Entity> displays, World world) {
        // 1) build the AABBs at the future anchor
        List<BoundingBox> boxes = computeBoundingBoxes(futureAnchor, displays);

        // 2) test each AABB against the world
        for (BoundingBox box : boxes) {
            int minX = (int) Math.floor(box.getMinX());
            int maxX = (int) Math.ceil (box.getMaxX());
            int minY = (int) Math.floor(box.getMinY());
            int maxY = (int) Math.ceil (box.getMaxY());
            int minZ = (int) Math.floor(box.getMinZ());
            int maxZ = (int) Math.ceil (box.getMaxZ());

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (!block.isPassable() && block.getType().isSolid()) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Compute an axis‐aligned bounding box for each Display entity,
     * given the ship’s anchor point in world coordinates.
     * These boxes can be used both for collision checks (above)
     * and for “on‐deck” foot detection.
     */
    @NotNull
    public static List<BoundingBox> computeBoundingBoxes(@NotNull Location anchor, List<Entity> entities) {
        List<BoundingBox> boxes = new ArrayList<>(entities.size());

        for (Entity e : entities) {
            if (!(e instanceof Display d)) continue;

            // 1) get the local translation (in blocks) from the Display's transformation
            Vector3f t = d.getTransformation().getTranslation();
            Vector offset = new Vector(t.x(), t.y(), t.z());

            // 2) compute the world‐space center of this display
            Vector center = anchor.toVector().add(offset);

            // 3) make a 1×1×1 box around that center, then inflate by PADDING
            BoundingBox box = BoundingBox.of(center, DISPLAY_PADDING,DISPLAY_PADDING,DISPLAY_PADDING);
            boxes.add(box);
        }

        return boxes;
    }
}
