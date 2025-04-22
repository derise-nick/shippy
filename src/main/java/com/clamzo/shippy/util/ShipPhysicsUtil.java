package com.clamzo.shippy.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ShipPhysicsUtil {

    public static boolean canMoveTo(Location futureLocation, Location currentLocation, List<Entity> displays, World world) {
        for (Entity entity : displays) {
            if (entity instanceof Display display) {
                Location displayLoc = display.getLocation();

                // Calculate future position of each display
                Vector offset = displayLoc.toVector().subtract(currentLocation.toVector());
                Vector otherOffset = Vector.fromJOML(display.getTransformation().getTranslation());
                Location futureDisplayLoc = futureLocation.clone().add(otherOffset);

                // We'll use a slightly inflated bounding box
                BoundingBox box = BoundingBox.of(futureDisplayLoc.toVector(), 0.75, 0.75, 0.75);
                // Check each block the bounding box overlaps
                for (int x = (int) box.getMinX(); x <= box.getMaxX(); x++) {
                    for (int y = (int) box.getMinY(); y <= box.getMaxY(); y++) {
                        for (int z = (int) box.getMinZ(); z <= box.getMaxZ(); z++) {
                            Block block = world.getBlockAt(x, y, z);
                            if (!block.isPassable() && !block.isEmpty()) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public static List<BoundingBox> computeBoundingBoxes(@NotNull Location location, List<Entity> entities) {
    }
}

