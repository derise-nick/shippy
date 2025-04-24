package com.clamzo.shippy.util;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.serialization.*;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class PortAndShipManager {
    private final ShippyPlugin plugin;
    private final ShipSerializer serializer;
    File dataFolder;
    File dataFile;
    private final Map<UUID, Location> portLocations = new HashMap<>();
    private final Map<UUID, Location> shipStructures = new HashMap<>();
    private final Map<UUID, ActiveShip> activeShips = new HashMap<>();
    private BukkitTask activeShipTask;

    public PortAndShipManager(ShippyPlugin plugin) {
        this.plugin = plugin;
        dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs(); // ✅ make the plugin folder if it doesn't exist
        }
        dataFile = new File(dataFolder, "ports.json");
        activeShipFile = new File(plugin.getDataFolder(), "active_ships.json");
        this.serializer = new ShipSerializer(plugin);
        shipGson = new GsonBuilder()
                .registerTypeAdapter(Location.class,   new LocationAdapter())
                .registerTypeAdapter(ItemStack.class, new ItemStackAdapter())
                .registerTypeAdapter(Vector3f.class, new Vector3fAdapter())
                .setPrettyPrinting()
                .create();
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File activeShipFile;
    private final Gson shipGson;

    public void saveActiveShips() {
        activeShipTask.cancel();
        Map<UUID, SerializableActiveShip> toSave = activeShips.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> serializeActiveShip(entry.getValue())));
        try (Writer writer = new FileWriter(activeShipFile)) {
            shipGson.toJson(toSave, writer);
            List<UUID> toRemove = new ArrayList<>(activeShips.keySet());

            for (UUID id : toRemove) {
                ActiveShip ship = activeShips.get(id);
                if (ship != null) {
                    removeActiveShip(id, ship);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save active ships: " + e.getMessage());
        }
    }

    public void loadActiveShips() {
        if (!activeShipFile.exists()) return;

        try (Reader reader = new FileReader(activeShipFile)) {
            Type type = new TypeToken<Map<UUID, SerializableActiveShip>>() {}.getType();
            Map<UUID, SerializableActiveShip> savedMap = shipGson.fromJson(reader, type);

            for (Map.Entry<UUID, SerializableActiveShip> entry : savedMap.entrySet()) {
                ActiveShip ship = deserializeActiveShip(entry.getValue());
                if (ship != null) {
                    activeShips.put(ship.getStandEntity().getUniqueId(), ship);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load active ships: " + e.getMessage());
        }
        plugin.getLogger().info("Active Ships: " + activeShips.toString());
    }

    private void debugJsonFailure(SerializableActiveShip.SerializedEntity obj) {
        try {
            if (obj.itemStack != null) plugin.getLogger().info("ItemStack: " + obj.itemStack);

            shipGson.toJson(obj); // don't need to save it, just serialize
        } catch (Exception ex) {
            plugin.getLogger().warning("Serialization failed for " + obj.getClass().getName() + ": " + ex);
            ex.printStackTrace();
        }
    }


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
        ship.getController().tick();
        ArmorStand stand = ship.getStandEntity();
        List<Entity> entities = ship.getEntities();

        // Base location = boat position
        Location base = stand.getLocation();

        for (Entity entity : entities) {
            if (entity instanceof Interaction) {
                entity.teleport(base.clone().add(-0.5, 1.5, 0));
                continue;
            }
            entity.teleport(base);
        }
    }


    public void addActiveShip(Player player, ArmorStand standEntity, List<Entity> entities, Display helmBlock) {
        if (standEntity == null) return;
        ActiveShip ship = new ActiveShip(player.getUniqueId(), standEntity, entities);
        activeShips.put(player.getUniqueId(), ship);
    }

    public ActiveShip getActiveShipForPlayerUUID(UUID uniqueId) {
        return activeShips.get(uniqueId);
    }

    public void activateAllShips() {
        activeShipTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            activeShips.forEach((id,ship) -> {
                moveDisplayShip(ship);
                ArmorStand seat = ship.getStandEntity();
                Vector shipVel = seat.getVelocity();
                List<BoundingBox> deckBoxes =
                        ShipPhysicsUtil.computeBoundingBoxes(seat.getLocation(), ship.getEntities());

                // for every player in that world
//                for (Player p : seat.getWorld().getPlayers()) {
//                    // approximate foot position slightly below eye level
//                    Vector footVec = p.getLocation().toVector().subtract(new org.bukkit.util.Vector(0, 0.1, 0));
//
//                    // if any box contains their foot
//                    boolean onDeck = deckBoxes.stream().anyMatch(bb -> bb.contains(footVec));
//                    if (onDeck) {
//                        // cancel any downward fall
//                        Vector v = p.getVelocity();
//                        v.setX(shipVel.getX());
//                        v.setZ(shipVel.getZ());
//                        if (v.getY() < 0) v.setY(0);
//                        p.setVelocity(v);
//                    }
//                }
            });
        },0L, 1L);
    }

    public ActiveShip getActiveShipForArmorStand(ArmorStand stand) {
        for (Map.Entry<UUID, ActiveShip> entry : activeShips.entrySet()) {
            if (entry.getValue().getStandEntity().equals(stand)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void removeActiveShip(UUID id, ActiveShip ship) {
        for (Entity entity : ship.getEntities()) {
            entity.remove();
        }
        activeShips.remove(id);
    }

    public Display getHelmBlock(List<Entity> displayList) {
        for (Entity entity : displayList) {
            if (entity instanceof ItemDisplay && ((ItemDisplay) entity).getItemStack().getType() == Material.COMPASS) {
                return (ItemDisplay) entity;
            }
        }
        return null;
    }

    public SerializableActiveShip serializeActiveShip(ActiveShip ship) {
        SerializableActiveShip shi = serializer.serializeShip(ship);
        shi.entities.forEach(this::debugJsonFailure);
        return shi;
    }

    public ActiveShip deserializeActiveShip(SerializableActiveShip data) {
        return serializer.deserializeShip(data);
    }

    public void removeActiveShipForArmorStand(ArmorStand stand) {
        UUID key = null;
        for (Map.Entry<UUID, ActiveShip> entry : activeShips.entrySet()) {
            if (entry.getValue().getStandEntity().equals(stand)) {
                key = entry.getKey();
            }
        }
        if (key != null) removeActiveShip(key, getActiveShipForArmorStand(stand));
        stand.remove();
    }

    public ActiveShip getShipNear(@NotNull Location location) {
        double distanceThreshold = 15.0; // adjust as needed
        for (ActiveShip ship : activeShips.values()) {
            Location helmLoc = ship.getStandEntity().getLocation();
            if (helmLoc.getWorld().equals(location.getWorld())
                    && helmLoc.distanceSquared(location) < distanceThreshold * distanceThreshold) {
                return ship;
            }
        }
        return null;
    }
}
