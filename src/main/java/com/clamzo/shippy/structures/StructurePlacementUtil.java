package com.clamzo.shippy.structures;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class StructurePlacementUtil {
    static public void placeStructure(Location base, int structWidth, int structLength, int structHeight, BlockFace facing) {
        int waterLevel = 2;
        World world = base.getWorld();

        for (int x = 0; x < structWidth; x++) {
            for (int z = 0; z < structLength; z++) {
                for (int y = 0; y < structHeight; y++) {
                    Location loc = offsetByFacing(base, x, y, z, facing);
                    Material mat;

                    // Floor layer = Stone Bricks
                    if (y == 0) {
                        mat = Material.STONE_BRICKS;
                    } else if ((x == 0 || x == structWidth - 1) && (z == 0 || z == structLength - 1)) {
                        if (y <= waterLevel){
                            mat = Material.CYAN_WOOL;
                        } else {
                            mat = Material.OAK_LOG;
                        }
                    } else {
                        mat = Material.AIR;
                    }

                    world.getBlockAt(loc).setType(mat);
                }
            }
        }

        Location buttonLoc = offsetByFacing(base, 1, 1, 0, facing);
        world.getBlockAt(buttonLoc).setType(Material.STONE_BUTTON);
    }

    static public BlockFace getCardinalFacingForPlayer(Player player) {
        return getCardinalFacing(player.getLocation());
    }

    static public BlockFace getCardinalFacing(Location loc) {
        float yaw = loc.getYaw();
        yaw = (yaw % 360 + 360) % 360; // Normalize
        if (yaw < 45 || yaw >= 315) return BlockFace.SOUTH;
        if (yaw < 135) return BlockFace.WEST;
        if (yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }

    static public float getYawFromFacing(String facing) {
        BlockFace bf = BlockFace.valueOf(facing);
        return switch (bf) {
            case BlockFace.SOUTH -> 0f;
            case BlockFace.WEST -> 90f;
            case BlockFace.NORTH -> 180f;
            case BlockFace.EAST -> 270f;
            default -> 0f;
        };
    }

    static public Location offsetByFacing(Location origin, int dx, int dy, int dz, BlockFace direction) {
        return switch (direction) {
            case NORTH -> origin.clone().add(-dx, dy, -dz);
            case SOUTH -> origin.clone().add(dx, dy, dz);
            case WEST -> origin.clone().add(-dz, dy, dx);
            case EAST -> origin.clone().add(dz, dy, -dx);
            default -> origin.clone().add(dx, dy, dz); // fallback
        };
    }
}
