package com.clamzo.shippy.behavior;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.util.ActiveShip;
import com.clamzo.shippy.util.PortAndShipManager;
import com.clamzo.shippy.util.SavedBlock;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.math.Rotations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShipInteractionListener implements Listener {
    private final PortAndShipManager manager;
    private final ShippyPlugin plugin;

    public ShipInteractionListener(ShippyPlugin plugin) {
        this.manager = plugin.manager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseShipItem(PlayerInteractEvent event) {
//        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
//
//        Player player = event.getPlayer();
//        ItemStack item = event.getItem();
//
//        if (item == null || item.getType() != Material.OAK_CHEST_BOAT) return;
//
//        ItemMeta meta = item.getItemMeta();
//        if (meta == null || !meta.hasDisplayName()) return;
//        if (!meta.getDisplayName().equalsIgnoreCase("Your Custom Ship")) return;

        // Cancel the normal boat placement
//        event.setCancelled(true);

//        Location base = player.getLocation().add(player.getLocation().getDirection().multiply(2)).getBlock().getLocation();

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
//        player.getInventory().setItemInMainHand(item);
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

            event.setCancelled(true);

            // Load their ship structure
            List<SavedBlock> saved = manager.loadShipStructure(nearest.getUniqueId());
            if (saved == null || saved.isEmpty()) {
                nearest.sendMessage(Component.text("No saved ship found.").color(NamedTextColor.RED));
                return;
            }
            // Translate the SavedBlocks into placed blocks relative to boat location
            Location baseLoc = boat.getLocation().getBlock().getLocation();
            World world = boat.getWorld();

            
            ArmorStand stand = (ArmorStand) boat.getWorld().spawnEntity(boat.getLocation().clone().add(0,-1,0), EntityType.ARMOR_STAND);
            stand.setInvisible(false);
            stand.setMarker(false);
            stand.setGravity(true);
            stand.setInvulnerable(true);
            stand.setCustomName("ShipController");
            stand.setCustomNameVisible(false);

            List<Display> displayList = spawnShipFromStructure(baseLoc, saved, stand);
            Display helmBlock = manager.getHelmBlock(displayList);
            // Register the active ship
            manager.addActiveShip(nearest, stand, displayList, helmBlock);
            nearest.sendMessage(Component.text("Ship deployed!").color(NamedTextColor.GREEN));
            boat.remove();

        }, 1L); // delay 1 tick
    }

    @EventHandler
    public void onInteractWithShip(PlayerInteractAtEntityEvent event) {
//        if (!(event.getRightClicked() instanceof ItemDisplay item)) return;
        Entity item = event.getRightClicked();
        NamespacedKey helmKey = new NamespacedKey(plugin, "ship_armorstand_uuid");
        PersistentDataContainer container = item.getPersistentDataContainer();

        if (!container.has(helmKey, PersistentDataType.STRING)) return;

        String uuidString = container.get(helmKey, PersistentDataType.STRING);
        UUID armorStandUUID = UUID.fromString(uuidString);

        plugin.getLogger().info("Armor stand key: " + armorStandUUID);

        ArmorStand stand = (ArmorStand) Bukkit.getEntity(armorStandUUID);
        if (stand == null) return;

        ActiveShip ship = manager.getActiveShipForArmorStand(stand);
        if (ship == null) return;

        Player player = event.getPlayer();
        stand.addPassenger(player);
        player.sendMessage(Component.text("Aye aye, Captain!").color(NamedTextColor.GREEN));
    }


    public List<Display> spawnShipFromStructure(Location origin, List<SavedBlock> structure, ArmorStand stand) {
        List<Display> displays = new ArrayList<>();
        World world = origin.getWorld();

        for (SavedBlock sb : structure) {
            Location spawnLoc = stand.getLocation().clone();

            // Relative offset from boat
            Vector offset = new Vector(sb.dx, sb.dy+1, sb.dz);

            // Spawn at the boat’s location
            BlockDisplay display = (BlockDisplay) world.spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
            display.setBlock(sb.getBlockData());
            display.setPersistent(true);
            display.setTeleportDuration(3); // Smooth movement

            // Apply relative offset as a transformation
            display.setTransformation(new Transformation(
                    offset.toVector3f(),                     // Translation (relative offset)
                    new AxisAngle4f(0, 0, 0, 0),             // No rotation (yet)
                    new Vector3f(1, 1, 1),                   // Scale = 1
                    new AxisAngle4f(0, 0, 0, 0)              // No rotation
            ));

            displays.add(display);
        }
        // Add helm directly above armor stand
        ItemStack helm = new ItemStack(Material.LECTERN, 1);
        ItemMeta helmMeta = helm.getItemMeta();
        helmMeta.setCustomModelData(313);
        helm.setItemMeta(helmMeta);
        ItemDisplay helmView = (ItemDisplay) world.spawnEntity(stand.getLocation(), EntityType.ITEM_DISPLAY);
        helmView.setItemStack(helm);

        helmView.setPersistent(true);
        helmView.setTeleportDuration(3);
        helmView.setTransformation(new Transformation(
                new Vector3f(0, 1.5f, 0),                    // Translation (relative offset)
                new AxisAngle4f(0, 0, 1, 0),             // No rotation (yet)
                new Vector3f(1, 1, 1),                   // Scale = 1
                new AxisAngle4f(0, 0, 0, 0)              // No rotation
        ));

        displays.add(helmView);

        Interaction interaction = (Interaction) world.spawnEntity(stand.getLocation().clone().add(0, 1, 0), EntityType.INTERACTION);
        interaction.setInteractionHeight(1.5f);
        interaction.setInteractionWidth(1.5f);
        interaction.setInvulnerable(true);
        interaction.setGravity(false);
        interaction.setPersistent(true);
        NamespacedKey helmKey = new NamespacedKey(plugin, "ship_armorstand_uuid");
        interaction.getPersistentDataContainer().set(helmKey, PersistentDataType.STRING, stand.getUniqueId().toString());
        interaction.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_helm"), PersistentDataType.BYTE, (byte) 1);

        return displays;
    }

//    @EventHandler
//    public void onBoatDestroyed(VehicleDestroyEvent event) {
//        if (!(event.getVehicle() instanceof ArmorStand stand)) return;
//
//        if (stand.getCustomName() == null || !stand.getCustomName().equals("ShipController")) return;
//
//        ActiveShip ship = manager.getActiveShipForBoat(stand);
//        if (ship == null) return;
//
//        for (BlockDisplay display : ship.getDisplayBlocks()) {
//            display.remove();
//        }
//
//        manager.removeActiveShip(stand);
//    }

    @EventHandler
    public void onArmorStandRemoved(EntityRemoveFromWorldEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;

        ActiveShip ship = manager.getActiveShipForArmorStand(stand);
        if (ship == null) return;

        for (Display display : ship.getDisplayBlocks()) {
            display.remove();
        }

        manager.removeActiveShip(stand);
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
