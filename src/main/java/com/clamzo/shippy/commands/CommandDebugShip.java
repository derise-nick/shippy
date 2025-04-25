package com.clamzo.shippy.commands;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.util.ActiveShip;
import com.clamzo.shippy.util.DebugVisualizer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class CommandDebugShip implements CommandExecutor {
    private final ShippyPlugin plugin;
    DebugVisualizer debugVisualizer;

    public CommandDebugShip(ShippyPlugin shippyPlugin, DebugVisualizer debugVisualizer) {
        plugin = shippyPlugin;
        this.debugVisualizer = debugVisualizer;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player player) {
            // Inside your command executor:
            if (debugVisualizer.isDebugging(player)) {
                debugVisualizer.disableDebugFor(player);
                player.sendMessage("§cShip debug disabled.");
            } else {
                ActiveShip ship = plugin.getManager().getShipNear(player.getLocation());
                if (ship != null) {
                    debugVisualizer.enableDebugFor(player, ship);
                    player.sendMessage("§aShip debug enabled. Sneak to view bounding boxes.");
                } else {
                    player.sendMessage("§eNo nearby ship to debug.");
                }
            }
            return true;
        }
        return false;
    }

}
