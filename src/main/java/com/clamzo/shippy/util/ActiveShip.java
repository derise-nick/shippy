package com.clamzo.shippy.util;

import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Boat;

import java.util.List;
import java.util.UUID;

public class ActiveShip {
    private final UUID ownerId;
    private final Boat boatEntity;
    private final List<BlockDisplay> displayBlocks;

    public ActiveShip(UUID ownerId, Boat boatEntity, List<BlockDisplay> displayBlocks) {
        this.ownerId = ownerId;
        this.boatEntity = boatEntity;
        this.displayBlocks = displayBlocks;
    }

    public UUID getOwnerId() { return ownerId; }
    public Boat getBoatEntity() { return boatEntity; }
    public List<BlockDisplay> getDisplayBlocks() { return displayBlocks; }
}


