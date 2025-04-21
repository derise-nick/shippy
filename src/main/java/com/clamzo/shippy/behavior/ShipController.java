package com.clamzo.shippy.behavior;

import com.clamzo.shippy.ShippyPlugin;
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
    private final double acceleration = 0.05;
    private final double maxSpeed = 0.4;
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

        if (playerPass.isEmpty()) { return; }
        plugin.getLogger().info("Old location: " + loc.toString());

        Player player = playerPass.get();
        boolean forward = player.getCurrentInput().isForward();

        // Turning
        if (player.getCurrentInput().isLeft()) yaw -= turnSpeed;
        if (player.getCurrentInput().isRight()) yaw += turnSpeed;
        plugin.getLogger().info("Player input: " + player.getCurrentInput());

        loc.setYaw(yaw);
        shipSeat.teleport(loc);

        // Forward movement
        if (forward) {
            Vector dir = loc.getDirection().normalize();
            plugin.getLogger().info("Velo before: " + velocity);
            velocity.add(dir.multiply(acceleration));
            plugin.getLogger().info("Velo after: " + velocity);
        }

        // Apply drag
        velocity.multiply(drag);

        // Clamp speed
        if (velocity.length() > maxSpeed) {
            velocity = velocity.normalize().multiply(maxSpeed);
        }

        // Apply movement
        Location newLoc = loc.add(velocity);
        plugin.getLogger().info("New location: " + newLoc.toString());
        shipSeat.teleport(newLoc);
    }
}
