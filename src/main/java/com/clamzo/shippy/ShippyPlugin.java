package com.clamzo.shippy;

import com.clamzo.shippy.behavior.ShipyardListener;
import com.clamzo.shippy.behavior.ShipInteractionListener;
import com.clamzo.shippy.commands.CommandDebugShip;
import com.clamzo.shippy.commands.CommandGivePort;
import com.clamzo.shippy.commands.CommandGiveShipyard;
import com.clamzo.shippy.util.DebugVisualizer;
import com.clamzo.shippy.util.PortAndShipManager;
import com.clamzo.shippy.util.ShipPhysicsUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ShippyPlugin extends JavaPlugin {
    private final File dataFolder = getDataFolder();
    public PortAndShipManager manager;
    private DebugVisualizer debugVisualizer;
    private ShipPhysicsUtil physicsUtil;

    @Override
    public void onEnable() {
        this.manager = new PortAndShipManager(this);
        debugVisualizer = new DebugVisualizer(this);
        getLogger().info("Shippy plugin has been enabled!");
        getCommand("giveshipyard").setExecutor(new CommandGiveShipyard());
        getCommand("giveport").setExecutor(new CommandGivePort());
        getCommand("shipdebug").setExecutor(new CommandDebugShip(this, debugVisualizer));
        getServer().getPluginManager().registerEvents(new ShipyardListener(this), this);
        getServer().getPluginManager().registerEvents(new ShipInteractionListener(this), this);
        manager.loadPortsFromDisk();
        manager.loadActiveShips();
        manager.activateAllShips();
        physicsUtil = new ShipPhysicsUtil(this);
        physicsUtil.activateDeckPhysics();
    }

    @Override
    public void onDisable() {
        getLogger().info("Shippy plugin has been disabled.");
        manager.savePortsToDisk();
        manager.saveActiveShips();
    }

    public PortAndShipManager getManager() {
        return this.manager;
    }
}