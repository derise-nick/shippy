package com.clamzo.shippy.structures;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class StructurePlacementUtil {
    static public void placeShipyard(Location base, int structWidth, int structLength, int structHeight, BlockFace facing) {
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

    static public ItemDisplay placePort(Location base, int structWidth, int structLength, int structHeight, BlockFace facing) {
        World world = base.getWorld();

        for (int x = 0; x < structWidth; x++) {
            for (int z = 0; z < structLength; z++) {
                if (z >= 3 && x >= 3 && x < structWidth -3) continue;
                for (int y = 0; y < structHeight; y++) {
                    Location loc = offsetByFacing(base, x, y, z, facing);
                    Material mat = Material.AIR;

                    // Floor layer = Stone Bricks
                    if (y == 0) {
                        mat = Material.DARK_OAK_PLANKS;
                    } else if (y==1) {
                        if ((z == 2 && x % 3 == 2) || (x == 2 || x == structWidth - 3) && z % 3 == 2) {
                            mat = Material.OAK_LOG;
                        } else {
                            mat = Material.DARK_OAK_SLAB;
                        }
                    }

                    world.getBlockAt(loc).setType(mat);
                }
            }
        }

        Location dockBlockLoc = offsetByFacing(base, 2, 2, 2, facing);
        world.getBlockAt(dockBlockLoc).setType(Material.DIRT);
        ItemDisplay display = (ItemDisplay) world.spawnEntity(dockBlockLoc, EntityType.ITEM_DISPLAY);
        Interaction interaction = (Interaction) world.spawnEntity(dockBlockLoc.clone().add(0.5,0.05,0.5), EntityType.INTERACTION);
        ItemStack dockBlock = new ItemStack(Material.FURNACE, 1);
        ItemMeta dockBlockMeta = dockBlock.getItemMeta();
        dockBlockMeta.setCustomModelData(313);
        dockBlock.setItemMeta(dockBlockMeta);
        display.setItemStack(dockBlock);
        display.setPersistent(true);
        display.setBrightness(new Display.Brightness(12,12));
        display.setTransformation(new Transformation(
                new Vector3f(0.5f,0.55f,0.5f),
                new AxisAngle4f(0, 0, 0, 0),
                new Vector3f(1.1f, 1.1f, 1.1f),
                new AxisAngle4f(0, 0, 0, 0)
        ));
        interaction.setPersistent(true);
        interaction.setInteractionHeight(1.1f);
        interaction.setInteractionWidth(1.1f);

        return display;
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

    public static boolean hasRoomForShipyard(Location baseLocation, BlockFace facing, int structureWidth, int structureLength, int structureHeight) {
        World world = baseLocation.getWorld();

        for (int x = 0; x < structureWidth; x++) {
            for (int z = 0; z < structureLength; z++) {
                for (int y = 0; y < structureHeight; y++) {
                    Location check = StructurePlacementUtil.offsetByFacing(baseLocation, x, y, z, facing);
                    Block block = world.getBlockAt(check);
                    if (!block.isPassable()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
