package com.clamzo.shippy.behavior;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.util.ActiveShip;
import com.clamzo.shippy.util.PortAndShipManager;
import com.clamzo.shippy.util.SavedBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
    private final PortAndShipManager shipManager;
    private final CustomBlockManager customBlockManager;
    private final ShippyPlugin plugin;

    public ShipInteractionListener(ShippyPlugin plugin) {
        this.shipManager = plugin.shipManager;
        this.customBlockManager = plugin.getCustomBlockManager();
        this.plugin = plugin;
    }

    @EventHandler
    public void onShipPlaced(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Material type = clickedBlock.getType();

        if (!(type == Material.WATER || type == Material.KELP || type == Material.SEAGRASS || type == Material.BUBBLE_COLUMN)) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        NamespacedKey shipKey = new NamespacedKey(plugin, "ship_id");
        if (!item.getPersistentDataContainer().has(shipKey, PersistentDataType.STRING)) return;

        UUID shipId = UUID.fromString(item.getPersistentDataContainer().get(shipKey, PersistentDataType.STRING));
        event.setCancelled(true);

        // Load their ship structure
        List<SavedBlock> saved = shipManager.getShipStructure(shipId);
        if (saved == null || saved.isEmpty()) {
            player.sendMessage(Component.text("No saved ship found.").color(NamedTextColor.RED));
            plugin.getLogger().warning("Error trying to place ship. No ship found for id: " + shipId);
            return;
        }
        SavedBlock helm = null;
        for (Iterator<SavedBlock> it = saved.iterator(); it.hasNext();) {
            SavedBlock value = it.next();
            if (value.getBlockData().getMaterial() == Material.OAK_FENCE && value.isCustomBlock) {
                helm = value;
            }
        }
        if (helm == null) return;
        Location baseLoc = clickedBlock.getLocation().clone().add(0,helm.dy,0);

        ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(baseLoc.clone().add(0, -1, 0), EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setMarker(false);
        stand.setGravity(true);
        stand.setInvulnerable(true);
        stand.setCustomName("ShipController");
        stand.setCustomNameVisible(false);
        spawnShipFromStructure(baseLoc, saved, stand, player);
    }
    
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
                ArmorStand stand = (ArmorStand) Bukkit.getEntity(UUID.fromString(
                        container.get(helmKey, PersistentDataType.STRING)));
                if (stand == null) return;
                if (container.has(helmKey, PersistentDataType.STRING)) {
                    ActiveShip ship = shipManager.getActiveShipForArmorStand(stand);
                    Location spawnLoc = item.getLocation().clone();
                    spawnLoc.setYaw(spawnLoc.getYaw() + 180);
                    switch (block.getBlock().getMaterial()) {
                        case Material.GRINDSTONE:
                            if (ship.fireCannon(item.getUniqueId())) item.getWorld().spawnEntity(spawnLoc, EntityType.FIREBALL);
                            break;
                        case Material.BARREL:
                            event.getPlayer().openInventory(ship.getInventory());
                            break;
                    }
                }
            }
        } else if (container.has(helmKey, PersistentDataType.STRING)) {
            String uuidString = container.get(helmKey, PersistentDataType.STRING);
            UUID armorStandUUID = UUID.fromString(uuidString);

            ArmorStand stand = (ArmorStand) Bukkit.getEntity(armorStandUUID);
            if (stand == null || !stand.getPassengers().isEmpty()) return;
            if (event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.DEBUG_STICK)) shipManager.removeActiveShipForArmorStand(stand);

            ActiveShip ship = shipManager.getActiveShipForArmorStand(stand);
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
        int helmHeight = 0;

        for (SavedBlock sb : structure) {
            if (sb.getBlockData().getMaterial() == Material.OAK_FENCE && sb.isCustomBlock) {
                helmHeight = sb.dy;
                continue;
            }
            Location spawnLoc = stand.getLocation().clone();

            // Relative offset from boat
            Vector offset = new Vector(sb.dx-0.5, sb.dy-0.25, sb.dz);

            // Spawn at the boat’s location
            BlockDisplay display = (BlockDisplay) world.spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
            display.setBlock(sb.getBlockData());
            display.setPersistent(true);
            display.setTeleportDuration(3);

            // Apply relative offset as a transformation
            display.setTransformation(new Transformation(
                    offset.toVector3f(),                     // Translation (relative offset)
                    new AxisAngle4f(0, 0, 0, 0),             // No rotation (yet)
                    new Vector3f(1, 1, 1),                   // Scale = 1
                    new AxisAngle4f(0, 0, 0, 0)              // No rotation
            ));

            entities.add(display);
            Material dispMat = sb.getBlockData().getMaterial();
            PersistentDataContainer pdc = display.getPersistentDataContainer();
            boolean isCustomBlock = (!pdc.has(new NamespacedKey(plugin, "custom_blockpos"), PersistentDataType.STRING));
            boolean isCannon = display.getBlock().getMaterial().equals(Material.GRINDSTONE) && isCustomBlock;
            if (isCannon || dispMat.equals(Material.BARREL)) {
                interactions.put(display.getUniqueId(), display);
            }
        }
        // Add helm directly above armor stand
        ItemStack helm = new ItemStack(Material.OAK_FENCE, 1);
        ItemMeta helmMeta = helm.getItemMeta();
        helmMeta.setCustomModelData(313);
        helm.setItemMeta(helmMeta);
        helmView.setItemStack(helm);

        helmView.setPersistent(true);
        helmView.setTeleportDuration(3);
        helmView.setTransformation(new Transformation(
                new Vector3f(0, 2.25f, 0.5f),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(1, 1, 1),
                new AxisAngle4f(0, 0, 0, 0)
        ));

        entities.add(helmView);

        Interaction helmInteraction = (Interaction) world.spawnEntity(stand.getLocation().clone().add(-0.5, 3.5, 0), EntityType.INTERACTION);
        helmInteraction.setInteractionHeight(1);
        helmInteraction.setInteractionWidth(1);
        helmInteraction.setInvulnerable(true);
        helmInteraction.setGravity(false);
        helmInteraction.setPersistent(true);
        NamespacedKey helmKey = new NamespacedKey(plugin, "ship_armorstand_uuid");
        UUID standId = stand.getUniqueId();
        helmInteraction.getPersistentDataContainer().set(helmKey, PersistentDataType.STRING, standId.toString());
        helmInteraction.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_helm"), PersistentDataType.BYTE, (byte) 1);
        entities.add(helmInteraction);

        List<Interaction> cannonList = new ArrayList<>();
        interactions.forEach((uid,display) -> {
            Interaction interaction = (Interaction) world.spawnEntity(display.getLocation(), EntityType.INTERACTION);
            interaction.setInteractionHeight(1f);
            interaction.setInteractionWidth(1f);
            interaction.setInvulnerable(true);
            interaction.setGravity(false);
            interaction.setPersistent(true);
            NamespacedKey dispId = new NamespacedKey(plugin, "display_uuid");
            interaction.getPersistentDataContainer().set(dispId, PersistentDataType.STRING, display.getUniqueId().toString());
            interaction.getPersistentDataContainer().set(helmKey, PersistentDataType.STRING, standId.toString());
            entities.add(interaction);
            if (display.getBlock().getMaterial() == Material.GRINDSTONE) {
                customBlockManager.spawnCannonDisplay(display, cannonList, interaction, entities);
            }
        });

        // Register the active ship
        ActiveShip newShip = shipManager.addActiveShip(standId, stand, entities, interactions, helmHeight);
        for (Interaction interaction : cannonList) {
            newShip.addCannon(interaction);
        }
        nearestPlayer.sendMessage(Component.text("Ship deployed!").color(NamedTextColor.GREEN));
    }
}
