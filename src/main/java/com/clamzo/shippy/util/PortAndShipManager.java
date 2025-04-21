package com.clamzo.shippy.util;

import com.clamzo.shippy.ShippyPlugin;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;

public class PortAndShipManager {
    private final ShippyPlugin plugin;
    File dataFolder;
    File dataFile;
    private final Map<UUID, Location> portLocations = new HashMap<>();
    private final Map<UUID, Location> shipStructures = new HashMap<>();
    private final Map<UUID, ActiveShip> activeShips = new HashMap<>();

    public PortAndShipManager(ShippyPlugin plugin) {
        this.plugin = plugin;
        dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs(); // ✅ make the plugin folder if it doesn't exist
        }
        dataFile = new File(dataFolder, "ports.json");
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void savePortsToDisk() {
        List<SavedPort> list = new ArrayList<>();
        for (Map.Entry<UUID, Location> entry : portLocations.entrySet()) {
            Location loc = entry.getValue();
            SavedPort sp = new SavedPort();
            sp.uuid = entry.getKey().toString();
            sp.x = loc.getX();
            sp.y = loc.getY();
            sp.z = loc.getZ();
            sp.world = loc.getWorld().getName();
            list.add(sp);
        }

        try {
            Files.createDirectories(dataFile.getParentFile().toPath());
            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(list, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadPortsFromDisk() {
        if (!dataFile.exists()) return;

        try (Reader reader = new FileReader(dataFile)) {
            Type listType = new TypeToken<List<SavedPort>>(){}.getType();
            List<SavedPort> ports = gson.fromJson(reader, listType);
            for (SavedPort sp : ports) {
                UUID id = UUID.fromString(sp.uuid);
                World world = Bukkit.getWorld(sp.world);
                if (world != null) {
                    Location loc = new Location(world, sp.x, sp.y, sp.z);
                    portLocations.put(id, loc);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addShipForUser(final UUID owner, List<SavedBlock> shipBlocks) {
        // Optional: Save to file under owner's UUID
        try (FileWriter writer = new FileWriter(new File(dataFolder, owner.toString() + "_ship.json"))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(shipBlocks, writer);
            Bukkit.getPlayer(owner).sendMessage(NamedTextColor.GREEN + "Ship structure saved!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, Location> getShipStructures() {
        return shipStructures;
    }
    public Map<UUID, Location> getPortLocations() {
        return portLocations;
    }

    public void addPortLocation(UUID playerId, Location loc) {
        portLocations.put(playerId, loc);
    }

    public List<SavedBlock> loadShipStructure(UUID uniqueId) {
        File shipFile = new File(dataFolder, uniqueId.toString() + "_ship.json");

        if (!shipFile.exists()) {
            return Collections.emptyList(); // or null if you prefer
        }

        try (Reader reader = new FileReader(shipFile)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<SavedBlock>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList(); // return empty if there's an issue
        }
    }

    public void moveDisplayShip(ActiveShip ship) {
        ArmorStand stand = ship.getStandEntity();
        List<BlockDisplay> displays = ship.getDisplayBlocks();

        // Base location = boat position
        Location base = stand.getLocation();

        for (BlockDisplay display : displays) {
            display.teleport(base);
        }
    }


    public void addActiveShip(Player player, ArmorStand standEntity, List<BlockDisplay> displayBlocks) {
        if (standEntity == null) return;
        // TODO: Make this persistent
        ActiveShip ship = new ActiveShip(player.getUniqueId(), standEntity, displayBlocks);
        activeShips.put(player.getUniqueId(), ship);
    }

    public ActiveShip getActiveShipForPlayerUUID(UUID uniqueId) {
        return activeShips.get(uniqueId);
    }

    public void activateAllShips() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            activeShips.forEach((s,v) -> moveDisplayShip(v));
        },0L, 2L);
    }

    public ActiveShip getActiveShipForArmorStand(ArmorStand stand) {
        for (Map.Entry<UUID, ActiveShip> entry : activeShips.entrySet()) {
            if (entry.getValue().getStandEntity().equals(stand)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void removeActiveShip(ArmorStand stand) {
        for (Map.Entry<UUID, ActiveShip> entry : activeShips.entrySet()) {
            if (entry.getValue().getStandEntity().equals(stand)) {
                activeShips.remove(entry.getKey());
            }
        }
    }

}
