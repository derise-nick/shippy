package com.clamzo.shippy.util;

import com.clamzo.shippy.ShippyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class ShipSerializer {
    private final ShippyPlugin plugin;
    public ShipSerializer(ShippyPlugin plugin) {
        this.plugin = plugin;
    }

    public ActiveShip deserializeShip(SerializableActiveShip data) {
        World world = Bukkit.getWorld(data.standLocation.getWorld().getUID());
        if (world == null) return null;

        // ArmorStand
        ArmorStand stand = world.spawn(data.standLocation, ArmorStand.class, a -> {
            a.setInvisible(true);
            a.setGravity(true);
            a.setMarker(true);
        });

        // BlockDisplays
        List<Entity> displays = new ArrayList<>();
        for (var s : data.entities) {
            String itemDisplay = ItemDisplay.class.toString();
            switch (s.entityType) {
                case itemDisplay:
                    ItemDisplay itemDisp = world.spawn(s.location, ItemDisplay.class);
                    itemDisp.setItemStack(s.itemStack);
                    itemDisp.setTransformation(new Transformation(s.transformation, new AxisAngle4f(), new Vector3f(1, 1, 1), new AxisAngle4f()));
                    break;
                case BlockDisplay.class:
                    BlockDisplay blockDisp = world.spawn(s.location, BlockDisplay.class);
                    blockDisp.setTransformation(new Transformation(s.transformation, new AxisAngle4f(), new Vector3f(1, 1, 1), new AxisAngle4f()));
                    break;
            }
            // TODO: Add these to a list and then use the spawn structure method from the interaction listener to ensure consistency
//            if (s.itemStack != null) {

//            } else if (s.transformation != null) {

//            } else {
//                Interaction disp = (Interaction) world.spawnEntity(s.location.clone().add(0, 1, 0), EntityType.INTERACTION);
//                disp.setInteractionHeight(1.5f);
//                disp.setInteractionWidth(1.5f);
//            }
//            displays.add(disp);
        }

        // HelmDisplay
        var helm = data.helmEntity;
        ItemDisplay helmDisp = world.spawn(helm.location, ItemDisplay.class);
        helmDisp.setItemStack(helm.itemStack);
        helmDisp.setTransformation(new Transformation(
                new Vector3f(0, 1.5f, 0),                    // Translation (relative offset)
                new AxisAngle4f(0, 0, 1, 0),             // No rotation (yet)
                new Vector3f(1, 1, 1),                   // Scale = 1
                new AxisAngle4f(0, 0, 0, 0)              // No rotation
        ));
        return new ActiveShip(data.ownerId, stand, displays, helmDisp);
    }
}
