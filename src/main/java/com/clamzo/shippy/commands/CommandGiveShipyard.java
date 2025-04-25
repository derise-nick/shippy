package com.clamzo.shippy.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class CommandGiveShipyard implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player player) {
            ItemStack portItem = new ItemStack(Material.BLAZE_POWDER);
            portItem.setAmount(1);
            ItemMeta portMeta = portItem.getItemMeta();
            portMeta.displayName(Component.text("Shipyard"));
            portItem.setItemMeta(portMeta);
            player.getInventory().addItem(portItem);
        }

        return true;
    }
}
