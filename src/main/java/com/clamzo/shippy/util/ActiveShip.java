package com.clamzo.shippy.util;

import com.clamzo.shippy.behavior.ShipController;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Display;

import java.util.List;
import java.util.UUID;

public class ActiveShip {
    private final UUID ownerId;
    private final ArmorStand standEntity;
    private final List<Display> displayBlocks;
    private final Display helmBlock;
    private final ShipController controller;

    public ActiveShip(UUID ownerId, ArmorStand standEntity, List<Display> displayBlocks, Display helmBlock) {
        this.ownerId = ownerId;
        this.standEntity = standEntity;
        this.displayBlocks = displayBlocks;
        this.helmBlock = helmBlock;
        this.controller = new ShipController(standEntity);
    }

    public UUID getOwnerId() { return ownerId; }
    public ArmorStand getStandEntity() { return standEntity; }
    public List<Display> getDisplayBlocks() { return displayBlocks; }
    public Display getHelmBlock() { return helmBlock; }
    public ShipController getController() { return controller; }
}


