package com.clamzo.shippy.behavior;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.util.ActiveShip;
import com.clamzo.shippy.util.PortAndShipManager;
import com.clamzo.shippy.util.SavedBlock;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class ShipInteractionListener implements Listener {
    private final PortAndShipManager manager;
    private final ShippyPlugin plugin;

    public ShipInteractionListener(ShippyPlugin plugin) {
        this.manager = plugin.manager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseShipItem(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.OAK_CHEST_BOAT) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        if (!meta.getDisplayName().equalsIgnoreCase("Your Custom Ship")) return;

        // Cancel the normal boat placement
//        event.setCancelled(true);

        Location base = player.getLocation().add(player.getLocation().getDirection().multiply(2)).getBlock().getLocation();

        // Check and load their saved ship
//        List<SavedBlock> ship = manager.loadShipStructure(player.getUniqueId());
//        if (ship == null || ship.isEmpty()) {
//            player.sendMessage(NamedTextColor.RED + "No saved ship found.");
//            return;
//        }
//        List<Block> shipBlocks = new ArrayList<Block>();
//
//        // Paste the ship at the new location
//        for (SavedBlock saved : ship) {
//            World world = base.getWorld();
//            Location loc = base.clone().add(saved.dx, saved.dy, saved.dz);
//            world.getBlockAt(loc).setType(Material.valueOf(saved.material));
//            shipBlocks.add(world.getBlockAt(loc));
//        }

//        item.setAmount(item.getAmount() - 1);
        player.getInventory().setItemInMainHand(item);
    }
    @EventHandler
    public void onBoatCreated(VehicleCreateEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;

        // Delay one tick to allow data to catch up (item used, etc.)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Check if the boat has a name (only our custom ships do)
            if (boat.getCustomName() == null || !boat.getCustomName().equals("Your Custom Ship")) return;

            // Find a nearby player (within 3 blocks) to associate with this boat
            Player nearest = boat.getWorld().getNearbyEntities(boat.getLocation(), 3, 3, 3).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .findFirst()
                    .orElse(null);

            if (nearest == null) return;

            // Load their ship structure
            List<SavedBlock> saved = manager.loadShipStructure(nearest.getUniqueId());
            if (saved == null || saved.isEmpty()) {
                nearest.sendMessage(NamedTextColor.RED + "No saved ship found.");
                return;
            }
            // Translate the SavedBlocks into placed blocks relative to boat location
            List<Block> shipBlocks = new ArrayList<>();
            Location baseLoc = boat.getLocation().getBlock().getLocation();

            List<BlockDisplay> displayList = spawnShipFromStructure(baseLoc, saved, boat);
            // Register the active ship
            manager.addActiveShip(nearest, boat, displayList);
            nearest.sendMessage(NamedTextColor.GREEN + "Ship deployed!");

        }, 1L); // delay 1 tick
    }



    @EventHandler
    public void onPlayerEnterBoat(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;
        if (!(event.getVehicle() instanceof Boat boat)) return;

        if (boat.getCustomName() == null || !boat.getCustomName().equals("Your Custom Ship")) return;

//        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
//            if (!boat.isValid() || !boat.getPassengers().contains(player)) return;
//
//            Vector direction = player.getLocation().getDirection().normalize().multiply(0.25);
//
//            ActiveShip ship = manager.getActiveShipForPlayerUUID(player.getUniqueId());
//            player.sendRichMessage("Attempting to move ship");
//
//
//        }, 0L, 2L); // 2 ticks = ~100ms
    }

    public List<BlockDisplay> spawnShipFromStructure(Location origin, List<SavedBlock> structure, Boat boat) {
        List<BlockDisplay> displays = new ArrayList<>();
        World world = origin.getWorld();

        for (SavedBlock sb : structure) {
            // Relative offset from boat
            Vector offset = new Vector(sb.dx, sb.dy-1.5, sb.dz);

            // Spawn at the boat’s location
            Location spawnLoc = boat.getLocation().clone();
            BlockDisplay display = (BlockDisplay) world.spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
            display.setBlock(sb.getBlockData());
            display.setPersistent(true);
            display.setInterpolationDuration(3); // Smooth movement
            display.setInterpolationDelay(0);

            // Apply relative offset as a transformation
            display.setTransformation(new Transformation(
                    offset.toVector3f(),                     // Translation (relative offset)
                    new AxisAngle4f(0, 0, 0, 0),             // No rotation (yet)
                    new Vector3f(1, 1, 1),                   // Scale = 1
                    new AxisAngle4f(0, 0, 0, 0)              // No rotation
            ));

            displays.add(display);
        }

        return displays;
    }

    @EventHandler
    public void onBoatDestroyed(VehicleDestroyEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;

        if (boat.getCustomName() == null || !boat.getCustomName().equals("Your Custom Ship")) return;

        ActiveShip ship = manager.getActiveShipForBoat(boat);
        if (ship == null) return;

        for (BlockDisplay display : ship.getDisplayBlocks()) {
            display.remove();
        }

        manager.removeActiveShip(boat);
    }



//    private void moveShip(ActiveShip ship, Vector movement, Player player) {
//        List<Block> currentBlocks = ship.getBlocks();
//
//        // Save current block states
//        List<Material> materials = currentBlocks.stream()
//                .map(Block::getType).toList();
//
//        // Clear old blocks
//        currentBlocks.forEach(b -> b.setType(Material.AIR));
//        currentBlocks.forEach(b -> player.sendRichMessage(b.toString()));
//
//        // Calculate new positions
//        List<Block> newBlocks = new ArrayList<>();
//        for (Block oldBlock : currentBlocks) {
//            Location newLoc = oldBlock.getLocation().add(movement);
//            Block newBlock = newLoc.getBlock();
//            newBlocks.add(newBlock);
//        }
//
//        // Set new blocks
//        for (int i = 0; i < newBlocks.size(); i++) {
//            newBlocks.get(i).setType(materials.get(i));
//        }
//
//        // Update state
//        ship.setBlocks(newBlocks);
//    }


}
