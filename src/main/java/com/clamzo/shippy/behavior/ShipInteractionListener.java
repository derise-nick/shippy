package com.clamzo.shippy.behavior;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.ship.ActiveShip;
import com.clamzo.shippy.util.PortAndShipManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

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
        shipManager.spawnShip(shipId, player, clickedBlock.getLocation());
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


}
