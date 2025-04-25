package com.clamzo.shippy.util;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class OrientedBoundingBox {
    private final Vector center;
    private final Vector halfExtents;
    private final double cosYaw, sinYaw;

    /**
     * @param center     center of the box in world coords
     * @param halfExtents half-widths along local X,Y,Z (e.g. .5, .5, .5 for a 1×1×1 block)
     * @param yawDegrees rotation around Y axis, in degrees (Minecraft yaw)
     */
    public OrientedBoundingBox(Vector center, Vector halfExtents, double yawDegrees) {
        this.center = center;
        this.halfExtents = halfExtents;
        double radians = Math.toRadians(yawDegrees);
        this.cosYaw = Math.cos(radians);
        this.sinYaw = Math.sin(radians);
    }

    /** Test whether a world-space point lies inside this OBB. */
    public boolean contains(Vector worldPoint) {
        // 1) translate into box-local coords
        Vector rel = worldPoint.clone().subtract(center);
        // 2) inverse-rotate around Y by –yaw
        double localX =  rel.getX() * cosYaw + rel.getZ() * sinYaw;
        double localY =  rel.getY();
        double localZ = -rel.getX() * sinYaw + rel.getZ() * cosYaw;
        // 3) test axis-aligned
        return  Math.abs(localX) <= halfExtents.getX()
                && Math.abs(localY) <= halfExtents.getY()
                && Math.abs(localZ) <= halfExtents.getZ();
    }

    /** Returns the 8 corner points of this box in world‐space. */
    public List<Vector> getCorners() {
        Vector he = halfExtents;
        // local corners before rotation
        Vector[] locals = {
                new Vector( he.getX(),  he.getY(),  he.getZ()),
                new Vector( he.getX(),  he.getY(), -he.getZ()),
                new Vector( he.getX(), -he.getY(),  he.getZ()),
                new Vector( he.getX(), -he.getY(), -he.getZ()),
                new Vector(-he.getX(),  he.getY(),  he.getZ()),
                new Vector(-he.getX(),  he.getY(), -he.getZ()),
                new Vector(-he.getX(), -he.getY(),  he.getZ()),
                new Vector(-he.getX(), -he.getY(), -he.getZ()),
        };

        List<Vector> worldCorners = new ArrayList<>(8);
        for (Vector local : locals) {
            // inverse‐rotate into world (apply +yaw)
            double x =  local.getX() * cosYaw - local.getZ() * sinYaw;
            double z =  local.getX() * sinYaw + local.getZ() * cosYaw;
            double y =  local.getY();

            // translate to center
            worldCorners.add(center.clone().add(new Vector(x, y, z)));
        }
        return worldCorners;
    }

}

