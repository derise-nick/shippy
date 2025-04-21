package com.clamzo.shippy.util;

import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Boat;

import java.util.List;
import java.util.UUID;

public class ActiveShip {
    private final UUID ownerId;
    private final ArmorStand standEntity;
    private final List<BlockDisplay> displayBlocks;

    public ActiveShip(UUID ownerId, ArmorStand standEntity, List<BlockDisplay> displayBlocks) {
        this.ownerId = ownerId;
        this.standEntity = standEntity;
        this.displayBlocks = displayBlocks;
    }

    public UUID getOwnerId() { return ownerId; }
    public ArmorStand getStandEntity() { return standEntity; }
    public List<BlockDisplay> getDisplayBlocks() { return displayBlocks; }
}


