package com.clamzo.shippy.util;

import com.clamzo.shippy.behavior.ShipController;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.UUID;

public class ActiveShip {
    private final UUID ownerId;
    private final ArmorStand standEntity;
    private final List<Entity> entities;
    private final Display helmBlock;
    private final ShipController controller;

    public ActiveShip(UUID ownerId, ArmorStand standEntity, List<Entity> entities, Display helmBlock) {
        this.ownerId = ownerId;
        this.standEntity = standEntity;
        this.entities = entities;
        this.helmBlock = helmBlock;
        this.controller = new ShipController(standEntity);
    }

    public UUID getOwnerId() { return ownerId; }
    public ArmorStand getStandEntity() { return standEntity; }
    public List<Entity> getEntities() { return entities; }
    public Display getHelmBlock() { return helmBlock; }
    public ShipController getController() { return controller; }
}


