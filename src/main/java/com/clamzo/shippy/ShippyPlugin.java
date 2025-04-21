package com.clamzo.shippy;

import com.clamzo.shippy.behavior.PortPlacementListener;
import com.clamzo.shippy.behavior.ShipInteractionListener;
import com.clamzo.shippy.commands.CommandGivePort;
import com.clamzo.shippy.util.ActiveShip;
import com.clamzo.shippy.util.PortAndShipManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ShippyPlugin extends JavaPlugin {
    private File dataFolder = getDataFolder();
    public PortAndShipManager manager;

    @Override
    public void onEnable() {
        this.manager = new PortAndShipManager(this);
        getLogger().info("Shippy plugin has been enabled!");
        this.getCommand("givePort").setExecutor(new CommandGivePort());
        getServer().getPluginManager().registerEvents(new PortPlacementListener(this), this);
        getServer().getPluginManager().registerEvents(new ShipInteractionListener(this), this);
        manager.loadPortsFromDisk();
        manager.activateAllShips();
    }

    @Override
    public void onDisable() {
        getLogger().info("Shippy plugin has been disabled.");
        manager.savePortsToDisk();
    }
}