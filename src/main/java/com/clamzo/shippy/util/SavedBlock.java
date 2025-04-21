package com.clamzo.shippy.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

public class SavedBlock {
    public int dx, dy, dz;
    private final String blockData;

    public SavedBlock(int x, int y, int z, BlockData data) {
        this.dx = x;
        this.dy = y;
        this.dz = z;
        this.blockData = data.getAsString(); // Save full block data string
    }

    public BlockData getBlockData() {
        return Bukkit.createBlockData(blockData); // Reconstruct it safely
    }

    public Location applyTo(Location origin) {
        return origin.clone().add(dx, dy, dz);
    }
}
