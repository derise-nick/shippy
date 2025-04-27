package com.clamzo.shippy.util;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.serialization.LocationAdapter;
import com.clamzo.shippy.serialization.Vector3fAdapter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
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
    private final Map<UUID, List<SavedBlock>> ships = new HashMap<>();


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
                .registerTypeAdapter(Vector3f.class, new Vector3fAdapter())
                .setPrettyPrinting()
                .create();
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File activeShipFile;
    private final Gson shipGson;
    private BukkitTask autoSaveTask;

    public void saveActiveShips() {
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
        plugin.getLogger().info("Active Ships: " + activeShips);
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
            sp.facing = StructurePlacementUtil.getCardinalFacing(loc).toString();
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
            plugin.getLogger().info("List of ports: " + ports);
            for (SavedPort sp : ports) {
                UUID id = UUID.fromString(sp.uuid);
                World world = Bukkit.getWorld(sp.world);
                if (world != null) {
                    Location loc = new Location(world, sp.x, sp.y, sp.z).setRotation(StructurePlacementUtil.getYawFromFacing(sp.facing), 0);
                    plugin.getLogger().info("Loading port location: " + loc);
                    portLocations.put(id, loc);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, Location> getShipyardLocations() {
        return portLocations;
    }

    public void addShipyardLocation(UUID playerId, Location loc) {
        portLocations.put(playerId, loc);
    }

    public void addShipForUser(final UUID shipId, List<SavedBlock> shipBlocks, Player player) {
        ships.put(shipId, shipBlocks);
        player.sendMessage(Component.text("Ship structure saved to memory!").color(NamedTextColor.GREEN));
    }

    public List<SavedBlock> getShipStructure(UUID uniqueId) {
        return ships.getOrDefault(uniqueId, Collections.emptyList());
    }

    public void saveAllShips() {
        File shipFile = new File(dataFolder, "ships.json");

        try (FileWriter writer = new FileWriter(shipFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(ships, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadAllShips() {
        File shipFile = new File(dataFolder, "ships.json");

        if (!shipFile.exists()) {
            return; // No ships saved yet
        }

        try (Reader reader = new FileReader(shipFile)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<UUID, List<SavedBlock>>>() {}.getType();
            Map<UUID, List<SavedBlock>> loadedShips = gson.fromJson(reader, type);

            if (loadedShips != null) {
                ships.clear();
                ships.putAll(loadedShips);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startAutoSaveTask(ShippyPlugin plugin, long intervalTicks) {
        autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                saveAllShips();
                savePortsToDisk();
                saveActiveShips();
            }
        }.runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);
    }

    public void stopTasks() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        if (activeShipTask != null) {
            activeShipTask.cancel();
        }
    }



    public void moveDisplayShip(ActiveShip ship) {
        ship.getController().tick();
        ArmorStand stand = ship.getStandEntity();
        List<Entity> entities = ship.getEntities();
        List<Interaction> cannonInteractions = new ArrayList<>();
        List<BlockDisplay> barrels = new ArrayList<>();

        // Base location = boat position
        Location base = stand.getLocation();

        for (Entity entity : entities) {

            if (entity instanceof Interaction interaction) {
                NamespacedKey helmFlag = new NamespacedKey(plugin, "is_helm");
                PersistentDataContainer container = entity.getPersistentDataContainer();
                if (container.has(helmFlag, PersistentDataType.BYTE)) {
                    entity.teleport(base.clone().add(-0.5, 1.5, 0));
                    continue;
                }
                addBoundInteraction(ship, interaction, stand);
                continue;
            }
            entity.teleport(base);
        }

    }

    private void addBoundInteraction(ActiveShip ship, Interaction interaction, ArmorStand stand) {
        NamespacedKey displayId = new NamespacedKey(plugin, "display_uuid");
        PersistentDataContainer container = interaction.getPersistentDataContainer();
        String id = container.get(displayId, PersistentDataType.STRING);
        BlockDisplay blockDisp = ship.getCannons().get(UUID.fromString(id));

        Vector3f t = blockDisp.getTransformation().getTranslation();
        Vector localOffset = new Vector(t.x()+0.5, t.y(), t.z()+0.5);

        float standYaw = stand.getLocation().getYaw();
        double rad = Math.toRadians(standYaw);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        Vector rotatedOffset = new Vector(
                localOffset.getX() * cos - localOffset.getZ() * sin,
                localOffset.getY(),
                localOffset.getX() * sin + localOffset.getZ() * cos
        );

        Location target = stand.getLocation().clone().add(rotatedOffset);

        Directional dir = (Directional) blockDisp.getBlock();
        BlockFace face = dir.getFacing();
        float faceYaw;
        switch (face) {
            case SOUTH: faceYaw =   0f; break;
            case WEST:  faceYaw =  90f; break;
            case NORTH: faceYaw = 180f; break;
            case EAST:  faceYaw = -90f; break;
            default:    faceYaw =    0f; break;
        }

        target.setYaw(standYaw + faceYaw);
        target.setPitch(0f);

        interaction.teleport(target);
    }


    public void addActiveShip(UUID standId, ArmorStand standEntity, List<Entity> entities, Map<UUID, BlockDisplay> cannons) {
        if (standEntity == null) return;
        ActiveShip ship = new ActiveShip(standId, standEntity, entities, cannons);
        activeShips.put(standId, ship);
    }

    public ActiveShip getActiveShipForPlayerUUID(UUID uniqueId) {
        return activeShips.get(uniqueId);
    }

    public void activateAllShips() {
        activeShipTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            activeShips.forEach((id,ship) -> {
                moveDisplayShip(ship);
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

    public ActiveShip getDeckForPlayer(@NotNull Player p) {
        for (Map.Entry<UUID, ActiveShip> entry : activeShips.entrySet()) {
            ActiveShip ship = entry.getValue();
            Vector footVec = p.getLocation().toVector().subtract(new org.bukkit.util.Vector(0, 0.1, 0));
            boolean onDeck = ship.getBoundingBoxes().stream().anyMatch(bb -> bb.contains(footVec));
            if (onDeck) return ship;
        }
        return null;
    }

    public Location findShipyardForLocation(Location loc) {
        for (Location shipyard: getShipyardLocations().values()) {
            if (shipyard.clone().setRotation(0f,0f).equals(loc)) return shipyard;
        }
        return null;
    }
}
