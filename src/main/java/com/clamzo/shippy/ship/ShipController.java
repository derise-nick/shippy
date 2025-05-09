package com.clamzo.shippy.ship;

import com.clamzo.shippy.ShippyPlugin;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Optional;

public class ShipController {
    private Vector velocity = new Vector(0, 0, 0);
    private final ArmorStand shipSeat;
    private final ActiveShip ship;
    private double acceleration = 0.004;
    private double maxSpeed = 1;
    private double drag = 0.98;
    private double turnSpeed = 2.5; // degrees per tick

    private double angularVelocity;

    public ShipController(ActiveShip ship) {
        this.shipSeat = ship.getStandEntity();
        this.ship = ship;
        this.plugin = JavaPlugin.getPlugin(ShippyPlugin.class);
        calibrateStats();
    }

    private void calibrateStats() {
        int blockCount = ship.getEntities().toArray().length;
        acceleration = Math.max(0.04 - blockCount/2000f,0.005);
        maxSpeed = Math.min(0.4 + blockCount/200f, 1);
        drag = Math.min(0.91 + blockCount/1000f,0.97);
        turnSpeed = Math.max(3.5 - blockCount/80f, 2.5);
    }

    private final ShippyPlugin plugin;
    public void tick() {
        ship.updateBoundingBoxes();
        ship.getCannons().forEach((id, cannon) -> cannon.tick());
        Location loc = shipSeat.getLocation();
        angularVelocity = 0;

        float yaw = loc.getYaw();
        Optional<Player> playerPass = shipSeat.getPassengers().stream().filter(pas -> pas instanceof Player).map(pas -> (Player) pas).findFirst();

        if (playerPass.isEmpty()) {
            velocity.setY(0);
            velocity.multiply(drag);
            shipSeat.setVelocity(velocity);
            return;
        }

        Player driver = playerPass.get();
        boolean forward = driver.getCurrentInput().isForward();
        boolean backward = driver.getCurrentInput().isBackward();

        // Turning
        if (driver.getCurrentInput().isLeft()) {
            yaw -= turnSpeed;
            driver.setRotation(driver.getYaw() - (float) turnSpeed, driver.getPitch());
            angularVelocity = Math.toRadians(turnSpeed);
        }
        if (driver.getCurrentInput().isRight()) {
            yaw += turnSpeed;
            driver.setRotation(driver.getYaw() + (float) turnSpeed, driver.getPitch());
            angularVelocity = 0-Math.toRadians(turnSpeed);
        }

        loc.setYaw(yaw);

        // Forward movement
        shipSeat.setRotation(yaw, shipSeat.getPitch());
        if (forward && !backward) {
            Vector dir = loc.getDirection().normalize();
            velocity.add(dir.multiply(acceleration));
        } else if (backward && !forward) {
            Vector dir = loc.getDirection().normalize();
            velocity.subtract(dir.multiply(acceleration*0.2));
        } else {
            // Apply drag
            velocity.multiply(drag);
        }

        // Clamp speed
        if (velocity.length() > maxSpeed) {
            velocity = velocity.normalize().multiply(maxSpeed);
        }
        Location predicted = loc.clone().add(velocity);

        if (ShipPhysicsUtil.canMoveTo(predicted, plugin.getShipManager().getActiveShipForArmorStand(shipSeat), shipSeat.getWorld())) {
            shipSeat.setVelocity(velocity);
        } else {
            velocity.zero();
            shipSeat.setVelocity(new Vector(0, 0, 0));
        }

    }

    public double getAngularVelocity() {
        return angularVelocity;
    }
}
