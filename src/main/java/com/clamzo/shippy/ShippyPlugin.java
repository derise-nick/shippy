package com.clamzo.shippy;

import com.clamzo.shippy.behavior.CustomBlockListener;
import com.clamzo.shippy.behavior.CustomBlockManager;
import com.clamzo.shippy.behavior.ShipyardListener;
import com.clamzo.shippy.behavior.ShipInteractionListener;
import com.clamzo.shippy.commands.CommandDebugShip;
import com.clamzo.shippy.commands.CommandGivePort;
import com.clamzo.shippy.commands.CommandGiveShipyard;
import com.clamzo.shippy.structures.StructurePreviewListener;
import com.clamzo.shippy.util.DebugVisualizer;
import com.clamzo.shippy.util.PortAndShipManager;
import com.clamzo.shippy.util.ShipPhysicsUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ShippyPlugin extends JavaPlugin {
    private final File dataFolder = getDataFolder();
    public PortAndShipManager shipManager;
    private CustomBlockManager customBlockManager;
    private DebugVisualizer debugVisualizer;
    private ShipPhysicsUtil physicsUtil;

    @Override
    public void onEnable() {
        this.shipManager = new PortAndShipManager(this);
        debugVisualizer = new DebugVisualizer(this);
        this.customBlockManager = new CustomBlockManager(this);
        getLogger().info("Shippy plugin has been enabled!");
        getCommand("giveshipyard").setExecutor(new CommandGiveShipyard());
        getCommand("giveport").setExecutor(new CommandGivePort());
        getCommand("shipdebug").setExecutor(new CommandDebugShip(this, debugVisualizer));
        getServer().getPluginManager().registerEvents(new ShipyardListener(this), this);
        getServer().getPluginManager().registerEvents(new StructurePreviewListener(this), this);
        getServer().getPluginManager().registerEvents(new ShipInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomBlockListener(this), this);
        shipManager.loadPortsFromDisk();
        shipManager.loadActiveShips();
        shipManager.activateAllShips();
        shipManager.loadAllShips();
        shipManager.startAutoSaveTask(this, 20L * 300L);
        physicsUtil = new ShipPhysicsUtil(this);
        physicsUtil.activateDeckPhysics();
        customBlockManager.loadCustomBlockModels();
    }

    @Override
    public void onDisable() {
        getLogger().info("Shippy plugin has been disabled.");
        shipManager.stopTasks();
        shipManager.savePortsToDisk();
        shipManager.saveActiveShips();
        shipManager.saveAllShips();
    }

    public PortAndShipManager getShipManager() {
        return this.shipManager;
    }
    public CustomBlockManager getCustomBlockManager() {
        return this.customBlockManager;
    }
}