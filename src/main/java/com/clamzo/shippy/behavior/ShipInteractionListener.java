package com.clamzo.shippy.behavior;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.util.ActiveShip;
import com.clamzo.shippy.util.PortAndShipManager;
import com.clamzo.shippy.util.SavedBlock;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

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

            ArmorStand stand = (ArmorStand) boat.getWorld().spawnEntity(baseLoc.clone().add(0,1.5f,0), EntityType.ARMOR_STAND);
            stand.setInvisible(true);
            stand.setMarker(false);
            stand.setGravity(true);
            stand.setInvulnerable(true);
            stand.setCustomName("ShipController");
            stand.setCustomNameVisible(false);

            spawnShipFromStructure(baseLoc, saved, stand, nearest);
            boat.remove();

        }, 1L); // delay 1 tick
    }

//    @EventHandler
//    public void onPlayerMove(PlayerMoveEvent event) {
//        Player player = event.getPlayer();
//        ActiveShip ship = manager.getDeckForPlayer(player);
//        if (ship == null) return;
//
//        // 1) read raw input
//        Input in = player.getCurrentInput();
//        Vector move = new Vector(0, 0, 0);
//
//        // 2) figure out the forward/left directions
//        float yaw = player.getYaw(); // I want the player's yaw, not the ship for now.
//        Vector forward = new Vector(
//                -Math.sin(Math.toRadians(yaw)),
//                0,
//                Math.cos(Math.toRadians(yaw))
//        );
//        forward.normalize();
//        Vector left = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
//
//        // 3) accumulate input
//        if (in.isForward())  move.add(forward);
//        if (in.isBackward()) move.subtract(forward);
//        if (in.isLeft())     move.subtract(left);
//        if (in.isRight())    move.add(left);
//
//        // 4) normalize & scale walking speed
//        if (move.lengthSquared() > 0) {
//            move.normalize().multiply(0.15);  // ~0.15 blocks/tick ≈ 3 blocks/sec
//        }
//
//        // 5) handle jumping
//        if (in.isJump()) {
//            move.setY(0.39);
//        } else if (player.isInWater()){
//            move.setY(0.06);
//        } else {
//            move.setY(0.09);;
//        }
//
//        // 6) apply it
//        player.setVelocity(move);
//    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!player.isInsideVehicle() || !(player.getVehicle() instanceof ArmorStand)) return;
        player.getVehicle().removePassenger(player);
    }

    @EventHandler
    public void onInteractWithShip(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction item)) return;
        NamespacedKey helmKey = new NamespacedKey(plugin, "ship_armorstand_uuid");
        NamespacedKey displayKey = new NamespacedKey(plugin, "display_uuid");
        PersistentDataContainer container = item.getPersistentDataContainer();

        if (container.has(displayKey, PersistentDataType.STRING)) {
            Entity refEntity = Bukkit.getEntity(UUID.fromString(container.get(displayKey, PersistentDataType.STRING)));
            if (refEntity instanceof BlockDisplay block) {
                switch (block.getBlock().getMaterial()) {
                    case Material.DISPENSER:
                        item.getWorld().spawnEntity(item.getLocation(), EntityType.FIREBALL);
                        break;
                    case Material.BARREL:
                        plugin.getLogger().info("Barrel accessed!");
                        if (container.has(helmKey, PersistentDataType.STRING)) {
                            plugin.getLogger().info("Barrel has helmKey!");
                            ArmorStand stand = (ArmorStand) Bukkit.getEntity(UUID.fromString(
                                container.get(helmKey, PersistentDataType.STRING)));
                            if (stand == null) return;
                            plugin.getLogger().info("Barrel armor stand found!");
                            ActiveShip ship = manager.getActiveShipForArmorStand(stand);
                            plugin.getLogger().info("Opening barrel for ship: " + ship.getOwnerId());
                            event.getPlayer().openInventory(ship.getInventory());
                        }
                        break;
                }
            }
        } else if (container.has(helmKey, PersistentDataType.STRING)) {
            String uuidString = container.get(helmKey, PersistentDataType.STRING);
            UUID armorStandUUID = UUID.fromString(uuidString);

            ArmorStand stand = (ArmorStand) Bukkit.getEntity(armorStandUUID);
            if (stand == null) return;
            if (event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.STICK)) manager.removeActiveShipForArmorStand(stand);

            ActiveShip ship = manager.getActiveShipForArmorStand(stand);
            if (ship == null) return;

            Player player = event.getPlayer();
            stand.addPassenger(player);
            player.setRotation(stand.getYaw(), player.getPitch());
            player.sendMessage(Component.text("Aye aye, Captain!").color(NamedTextColor.GREEN));
        }
    }


    public void spawnShipFromStructure(Location origin, List<SavedBlock> structure, ArmorStand stand, Player nearestPlayer) {
        List<Entity> entities = new ArrayList<>();
        Map<UUID, BlockDisplay> interactions = new HashMap<>();
        World world = origin.getWorld();
        ItemDisplay helmView = (ItemDisplay) world.spawnEntity(stand.getLocation(), EntityType.ITEM_DISPLAY);

        for (SavedBlock sb : structure) {
            Location spawnLoc = stand.getLocation().clone();

            // Relative offset from boat
            Vector offset = new Vector(sb.dx-0.5, sb.dy+1.5, sb.dz);

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

            entities.add(display);
            Material dispMat = sb.getBlockData().getMaterial();
            if (dispMat.equals(Material.DISPENSER) || dispMat.equals(Material.BARREL)) {
                interactions.put(display.getUniqueId(), display);
            }
        }
        // Add helm directly above armor stand
        ItemStack helm = new ItemStack(Material.LECTERN, 1);
        ItemMeta helmMeta = helm.getItemMeta();
        helmMeta.setCustomModelData(313);
        helm.setItemMeta(helmMeta);
        helmView.setItemStack(helm);

        helmView.setPersistent(true);
        helmView.setTeleportDuration(3);
        helmView.setTransformation(new Transformation(
                new Vector3f(0, 2f, 0.5f),                    // Translation (relative offset)
                new AxisAngle4f(0, 0, 1, 0),             // No rotation (yet)
                new Vector3f(1, 1, 1),                   // Scale = 1
                new AxisAngle4f(0, 0, 0, 0)              // No rotation
        ));

        entities.add(helmView);

        Interaction helmInteraction = (Interaction) world.spawnEntity(stand.getLocation().clone().add(-0.5, 1.5, 0), EntityType.INTERACTION);
        helmInteraction.setInteractionHeight(1.5f);
        helmInteraction.setInteractionWidth(1.5f);
        helmInteraction.setInvulnerable(true);
        helmInteraction.setGravity(false);
        helmInteraction.setPersistent(true);
        NamespacedKey helmKey = new NamespacedKey(plugin, "ship_armorstand_uuid");
        helmInteraction.getPersistentDataContainer().set(helmKey, PersistentDataType.STRING, stand.getUniqueId().toString());
        helmInteraction.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_helm"), PersistentDataType.BYTE, (byte) 1);
        entities.add(helmInteraction);

        interactions.forEach((uid,display) -> {
            Interaction interaction = (Interaction) world.spawnEntity(display.getLocation(), EntityType.INTERACTION);
            interaction.setInteractionHeight(1f);
            interaction.setInteractionWidth(1f);
            interaction.setInvulnerable(true);
            interaction.setGravity(false);
            interaction.setPersistent(true);
            NamespacedKey dispId = new NamespacedKey(plugin, "display_uuid");
            interaction.getPersistentDataContainer().set(dispId, PersistentDataType.STRING, display.getUniqueId().toString());
            interaction.getPersistentDataContainer().set(helmKey, PersistentDataType.STRING, stand.getUniqueId().toString());
            entities.add(interaction);
        });


        // Register the active ship
        manager.addActiveShip(nearestPlayer, stand, entities, interactions);
        nearestPlayer.sendMessage(Component.text("Ship deployed!").color(NamedTextColor.GREEN));
    }



//    @EventHandler
    public void onArmorStandRemoved(EntityRemoveFromWorldEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;
        manager.removeActiveShipForArmorStand(stand);
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
