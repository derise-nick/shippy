package com.clamzo.shippy.ship;

import java.util.UUID;

public class Cannon {
    private final UUID shipId;
    private final UUID interactionId;

    private int ammo;
    private final int cooldownTicks;
    private int cooldownRemaining;

    public Cannon(UUID shipId, UUID displayId, int initialAmmo, int cooldownTicks) {
        this.shipId = shipId;
        this.interactionId = displayId;
        this.ammo = initialAmmo;
        this.cooldownTicks = cooldownTicks;
        this.cooldownRemaining = 0;
    }

    public boolean fire() {
        if (cooldownRemaining > 0 || ammo <= 0) {
            return false;
        }

        ammo--;
        cooldownRemaining = cooldownTicks;
        return true;
    }

    public void tick() {
        if (cooldownRemaining > 0) {
            cooldownRemaining--;
        }
    }

    public boolean isReadyToFire() {
        return cooldownRemaining == 0 && ammo > 0;
    }

    public UUID getShipId() {
        return shipId;
    }

    public UUID getInteractionId() {
        return interactionId;
    }

    public int getAmmo() {
        return ammo;
    }

    public void addAmmo(int amount) {
        ammo += amount;
    }

    public int getCooldownRemaining() {
        return cooldownRemaining;
    }

    public void resetCooldown() {
        cooldownRemaining = 0;
    }
}

