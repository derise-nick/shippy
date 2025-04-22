package com.clamzo.shippy.behavior;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.util.ShipPhysicsUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Optional;

public class ShipController {
    private Vector velocity = new Vector(0, 0, 0);
    private final ArmorStand shipSeat;
    private final double acceleration = 0.04;
    private final double maxSpeed = 0.8;
    private final double drag = 0.91;
    private final double turnSpeed = 3.5; // degrees per tick

    public ShipController(ArmorStand seat) {
        this.shipSeat = seat;
        // TODO: Remove plugin after debugging
        this.plugin = JavaPlugin.getPlugin(ShippyPlugin.class);
    }

    private final ShippyPlugin plugin;
    public void tick() {

        Location loc = shipSeat.getLocation();

        float yaw = loc.getYaw();
        Optional<Player> playerPass = shipSeat.getPassengers().stream().filter(pas -> pas instanceof Player).map(pas -> (Player) pas).findFirst();

        if (playerPass.isEmpty()) {
            velocity.setY(0);
            velocity.multiply(drag);
            shipSeat.setVelocity(velocity);
            return;
        }

        Player player = playerPass.get();
        boolean forward = player.getCurrentInput().isForward();

        // Turning
        if (player.getCurrentInput().isLeft()) yaw -= turnSpeed;
        if (player.getCurrentInput().isRight()) yaw += turnSpeed;

        loc.setYaw(yaw);

        // Forward movement
        shipSeat.setRotation(yaw, shipSeat.getPitch());
        if (forward) {
            Vector dir = loc.getDirection().normalize();
            velocity.add(dir.multiply(acceleration));
        }

        // Apply drag
        velocity.multiply(drag);

        // Clamp speed
        if (velocity.length() > maxSpeed) {
            velocity = velocity.normalize().multiply(maxSpeed);
        }
        Location predicted = loc.clone().add(velocity);

        if (ShipPhysicsUtil.canMoveTo(predicted, loc, plugin.getManager().getActiveShipForArmorStand(shipSeat).getDisplayBlocks(), loc.getWorld())) {
            // Allow movement
            velocity.setY(0);
            shipSeat.setVelocity(velocity);
        } else {
            // Cancel movement
            velocity.zero();
            shipSeat.setVelocity(new Vector(0, 0, 0));
        }

    }
}
