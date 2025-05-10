package com.clamzo.shippy.behavior;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.structures.StructurePlacementUtil;
import com.clamzo.shippy.util.PortAndShipManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PortListener implements Listener {

    private final ShippyPlugin plugin;
    private final CustomBlockManager customBlockManager;
    private final PortAndShipManager portManager;

    public PortListener(ShippyPlugin plugin) {
        this.plugin = plugin;
        this.portManager = plugin.getShipManager();
        this.customBlockManager = plugin.getCustomBlockManager();
    }

    @EventHandler
    public void onPortItemUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BLAZE_POWDER) return;

        // Check item name
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        if (!meta.getDisplayName().equalsIgnoreCase("Port")) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        BlockFace facing = StructurePlacementUtil.getCardinalFacingForPlayer(player);

        // Get the base location (one block above clicked surface)
        Location baseLocation = clickedBlock.getRelative(facing).getLocation();

        if (!StructurePlacementUtil.hasRoomForShipyard(baseLocation, facing, 20, 16, 2)) {
            player.sendMessage(Component.text("Not enough space to place the Port.").color(NamedTextColor.RED));
            return;
        }
        ItemDisplay itemDisplay = StructurePlacementUtil.placePort(baseLocation, 20, 16, 2, facing);
        NamespacedKey posKey = new NamespacedKey(plugin, "custom_blockpos");

        String posString = (int) Math.floor(itemDisplay.getX()) + ","
                + (int) Math.floor(itemDisplay.getY()) + ","
                + (int) Math.floor(itemDisplay.getZ()) + ","
                + itemDisplay.getWorld().getUID();
        itemDisplay.getPersistentDataContainer().set(posKey, PersistentDataType.STRING, posString);
        customBlockManager.addCustomItemDisplay(itemDisplay.getLocation(), itemDisplay);
        this.portManager.addPortLocation(itemDisplay.getUniqueId(), itemDisplay.getLocation().clone().setRotation(StructurePlacementUtil.getYawFromFacing(facing.toString()), 0));
        player.sendMessage(Component.text("Port placed successfully!").color(NamedTextColor.GREEN));
    }

    @EventHandler
    public void onPortOpened(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction interaction)) return;
        Location interactionLoc = interaction.getLocation();
        ItemDisplay itemDisplay = customBlockManager.getItemDisplayForLocation(interactionLoc);
        if (itemDisplay == null || !itemDisplay.getItemStack().getItemMeta().hasCustomModelData()) return;
        if (!(itemDisplay.getItemStack().getItemMeta().getCustomModelData() == 313) && itemDisplay.getItemStack().getType() == Material.FURNACE) return;
        Inventory dockInv = Bukkit.createInventory(null, 9, Component.text("Choose a ship to launch!"));
        ItemStack launchButton = new ItemStack(Material.COAL, 1);
        ItemMeta meta = launchButton.getItemMeta();
        meta.displayName(Component.text("Launch"));
        // Set the lore instructions
        List<Component> lore = List.of(
                Component.text("Choose a ship from your", NamedTextColor.GRAY),
                Component.text("inventory and place it", NamedTextColor.GRAY),
                Component.text("in the empty slot.", NamedTextColor.GRAY),
                Component.text("Then click 'Launch'.", NamedTextColor.GRAY)
        );
        meta.lore(lore);

        meta.setCustomModelData(313);
        launchButton.setItemMeta(meta);

        meta.setCustomModelData(313);
        launchButton.setItemMeta(meta);
        ItemStack disabledPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta disabledMeta = disabledPane.getItemMeta();
        disabledMeta.setCustomModelData(313);
        disabledPane.setItemMeta(disabledMeta);
        dockInv.setContents(Collections.nCopies(9, disabledPane).toArray(new ItemStack[0]));
        dockInv.setItem(5, launchButton);
        dockInv.setItem(3, null);
        event.getPlayer().openInventory(dockInv);
    }

    @EventHandler
    public void onShipLaunch(InventoryClickEvent event) {
        if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) return;
        ItemStack item = event.getCurrentItem();
        boolean isShippyItem = item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 313;
        if (item.getType() == Material.GRAY_STAINED_GLASS_PANE && isShippyItem) event.setCancelled(true);
        if (item.getType() != Material.COAL) return;

        if (isShippyItem) {
            event.setCancelled(true);
            ItemStack itemStack = Arrays.stream(event.getInventory().getContents()).filter(stack -> (stack != null && stack.getType() == Material.OAK_CHEST_BOAT)).findFirst().orElse(null);
            if (itemStack == null || !itemStack.getItemMeta().hasCustomName()) return;
            NamespacedKey key = new NamespacedKey(plugin, "ship_id");
            if (!itemStack.getItemMeta().getPersistentDataContainer().has(key)) return;
            String shipId = itemStack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (shipId != null && event.getWhoClicked() instanceof Player player) {
                portManager.spawnShip(UUID.fromString(shipId), player, getLaunchLocation(player));
            }
            event.getInventory().close();
        }
    }

    private Location getLaunchLocation(Player player) {
        ItemDisplay nearestDisplay = player.getWorld().getNearbyEntities(player.getLocation(), 3, 3, 3).stream()
                .filter(e -> e instanceof ItemDisplay)
                .map(e -> (ItemDisplay) e)
                .findFirst()
                .orElse(null);
        if (nearestDisplay == null) {
            plugin.getLogger().warning("Attempted to find launch location without ItemDisplay!");
            return null;
        }
        Location portLoc = portManager.getPortForDisplayId(nearestDisplay);
        if (portLoc == null) {
            plugin.getLogger().warning("No port found for item display!");
            return null;
        }
        return StructurePlacementUtil.offsetByFacing(portLoc, 2, -2, 2, StructurePlacementUtil.getCardinalFacing(portLoc));
    }

    @EventHandler
    public void onDockClose(InventoryCloseEvent event) {
        ItemStack item = event.getInventory().getItem(5);
        if (item == null || item.getType() != Material.COAL) return;
        boolean isShippyItem = item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 313;
        if (!isShippyItem) return;
        ItemStack shipItem = event.getInventory().getItem(3);
        if (shipItem != null) event.getPlayer().getInventory().addItem(shipItem);
    }
}
