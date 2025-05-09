package com.clamzo.shippy.util;

import com.clamzo.shippy.ShippyPlugin;
import com.clamzo.shippy.behavior.CustomBlockManager;
import com.clamzo.shippy.serialization.LocationAdapter;
import com.clamzo.shippy.serialization.Vector3fAdapter;
import com.clamzo.shippy.ship.ActiveShip;
import com.clamzo.shippy.ship.SerializableActiveShip;
import com.clamzo.shippy.ship.ShipSerializer;
import com.clamzo.shippy.structures.SavedPort;
import com.clamzo.shippy.structures.StructurePlacementUtil;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
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
    private final CustomBlockManager customBlockManager;
    File dataFolder;
    File dataFile;
    private final Map<UUID, Location> shipyardLocations = new HashMap<>();
    private final Map<UUID, Location> portLocations = new HashMap<>();
    private final Map<UUID, Location> shipStructures = new HashMap<>();
    private final Map<UUID, ActiveShip> activeShips = new HashMap<>();
    private BukkitTask activeShipTask;
    private final Map<UUID, List<SavedBlock>> ships = new HashMap<>();


    public PortAndShipManager(ShippyPlugin plugin) {
        this.plugin = plugin;
        this.customBlockManager = plugin.getCustomBlockManager();
        dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
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
        try {
            saveActiveShipsAsync();

            List<UUID> toRemove = new ArrayList<>(activeShips.keySet());
            for (UUID id : toRemove) {
                ActiveShip ship = activeShips.get(id);
                if (ship != null) {
                    removeActiveShip(id, ship);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save active ships during disable: " + e.getMessage());
        }
    }
    private void saveActiveShipsAsync() throws IOException {
        Map<UUID, SerializableActiveShip> toSave = activeShips.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> serializeActiveShip(entry.getValue())));
        try (Writer writer = new FileWriter(activeShipFile)) {
            shipGson.toJson(toSave, writer);
        } catch (IOException e) {
            throw new IOException(e);
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
        for (Map.Entry<UUID, Location> entry : shipyardLocations.entrySet()) {
            Location loc = entry.getValue();
            SavedPort sp = new SavedPort();
            sp.uuid = entry.getKey().toString();
            sp.x = loc.getX();
            sp.y = loc.getY();
            sp.z = loc.getZ();
            sp.world = loc.getWorld().getName();
            sp.facing = StructurePlacementUtil.getCardinalFacing(loc).toString();
            sp.type = "shipyard";
            list.add(sp);
        }
        for (Map.Entry<UUID, Location> entry : portLocations.entrySet()) {
            Location loc = entry.getValue();
            SavedPort sp = new SavedPort();
            sp.uuid = entry.getKey().toString();
            sp.x = loc.getX();
            sp.y = loc.getY();
            sp.z = loc.getZ();
            sp.world = loc.getWorld().getName();
            sp.facing = StructurePlacementUtil.getCardinalFacing(loc).toString();
            sp.type = "port";
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
                    Location loc = new Location(world, sp.x, sp.y, sp.z).setRotation(StructurePlacementUtil.getYawFromFacing(sp.facing), 0);
                    if (sp.type.equals("shipyard")) shipyardLocations.put(id, loc);
                    else portLocations.put(id,loc);
                }
            }
            plugin.getLogger().info("shipyard locs: " + shipyardLocations);
            plugin.getLogger().info("port locs: " + portLocations);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, Location> getShipyardLocations() {
        return shipyardLocations;
    }

    public void addShipyardLocation(UUID playerId, Location loc) {
        shipyardLocations.put(playerId, loc);
    }

    public Map<UUID, Location> getPortLocations() {
        return portLocations;
    }

    public void addPortLocation(UUID interactionId, Location loc) {
        portLocations.put(interactionId, loc);
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
                try {
                    saveActiveShipsAsync();
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to save active ships during backup job: " + e.getMessage());
                }
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

        // Base location = boat position
        Location base = stand.getLocation();

        for (Entity entity : entities) {

            if (entity instanceof Interaction interaction) {
                NamespacedKey helmFlag = new NamespacedKey(plugin, "is_helm");
                PersistentDataContainer container = entity.getPersistentDataContainer();
                if (container.has(helmFlag, PersistentDataType.BYTE)) {
                    entity.teleport(base.clone().add(0,1.75,0));
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
        BlockDisplay blockDisp = ship.getInteractionRefs().get(UUID.fromString(id));
        if (blockDisp == null) {
            return;
        }
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


    public ActiveShip addActiveShip(UUID standId, @NotNull ArmorStand standEntity, List<Entity> entities, Map<UUID, BlockDisplay> interactionRefs, int helmHeight) {
        ActiveShip ship = new ActiveShip(standId, standEntity, entities, interactionRefs, helmHeight, plugin);
        activeShips.put(standId, ship);
        return ship;
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
        //        shi.entities.forEach(this::debugJsonFailure);   // uncomment this line to debug serialization
        return serializer.serializeShip(ship);
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

    public void spawnShipEntitiesFromStructure(Location origin, List<SavedBlock> structure, ArmorStand stand, Player nearestPlayer) {
        List<Entity> entities = new ArrayList<>();
        Map<UUID, BlockDisplay> interactions = new HashMap<>();
        World world = origin.getWorld();
        ItemDisplay helmView = (ItemDisplay) world.spawnEntity(stand.getLocation(), EntityType.ITEM_DISPLAY);
        int helmHeight = 0;

        for (SavedBlock sb : structure) {
            if (sb.getBlockData().getMaterial() == Material.OAK_FENCE && sb.isCustomBlock) {
                helmHeight = sb.dy;
                continue;
            }
            Location spawnLoc = stand.getLocation().clone();

            // Relative offset from boat
            BlockFace facing = BlockFace.valueOf(sb.facing);
            Vector rotatedOffset = getRotatedVector(sb, facing);

            // Spawn at the boat’s location
            BlockDisplay display = (BlockDisplay) world.spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
            display.setBlock(sb.getBlockData());
            display.setPersistent(true);
            display.setTeleportDuration(3);

            // Apply relative offset as a transformation
            display.setTransformation(new Transformation(
                    rotatedOffset.toVector3f(),                     // Translation (relative offset)
                    new AxisAngle4f(0, 0, 0, 0),             // No rotation (yet)
                    new Vector3f(1, 1, 1),                   // Scale = 1
                    new AxisAngle4f(0, 0, 0, 0)              // No rotation
            ));

            entities.add(display);
            Material dispMat = sb.getBlockData().getMaterial();
            PersistentDataContainer pdc = display.getPersistentDataContainer();
            boolean isCustomBlock = (!pdc.has(new NamespacedKey(plugin, "custom_blockpos"), PersistentDataType.STRING));
            boolean isCannon = display.getBlock().getMaterial().equals(Material.GRINDSTONE) && isCustomBlock;
            if (isCannon || dispMat.equals(Material.BARREL)) {
                interactions.put(display.getUniqueId(), display);
            }
        }
        // Add helm directly above armor stand
        ItemStack helm = new ItemStack(Material.OAK_FENCE, 1);
        ItemMeta helmMeta = helm.getItemMeta();
        helmMeta.setCustomModelData(313);
        helm.setItemMeta(helmMeta);
        helmView.setItemStack(helm);

        helmView.setPersistent(true);
        helmView.setTeleportDuration(3);
        helmView.setTransformation(new Transformation(
                new Vector3f(0, 2.25f, 0.5f),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(1, 1, 1),
                new AxisAngle4f(0, 0, 0, 0)
        ));

        entities.add(helmView);

        Interaction helmInteraction = (Interaction) world.spawnEntity(stand.getLocation().clone().add(-0.5, 3.5, 0), EntityType.INTERACTION);
        helmInteraction.setInteractionHeight(1);
        helmInteraction.setInteractionWidth(1);
        helmInteraction.setInvulnerable(true);
        helmInteraction.setGravity(false);
        helmInteraction.setPersistent(true);
        NamespacedKey helmKey = new NamespacedKey(plugin, "ship_armorstand_uuid");
        UUID standId = stand.getUniqueId();
        helmInteraction.getPersistentDataContainer().set(helmKey, PersistentDataType.STRING, standId.toString());
        helmInteraction.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_helm"), PersistentDataType.BYTE, (byte) 1);
        entities.add(helmInteraction);

        List<Interaction> cannonList = new ArrayList<>();
        interactions.forEach((uid,display) -> {
            Interaction interaction = (Interaction) world.spawnEntity(display.getLocation(), EntityType.INTERACTION);
            interaction.setInteractionHeight(1f);
            interaction.setInteractionWidth(1f);
            interaction.setInvulnerable(true);
            interaction.setGravity(false);
            interaction.setPersistent(true);
            NamespacedKey dispId = new NamespacedKey(plugin, "display_uuid");
            interaction.getPersistentDataContainer().set(dispId, PersistentDataType.STRING, display.getUniqueId().toString());
            interaction.getPersistentDataContainer().set(helmKey, PersistentDataType.STRING, standId.toString());
            entities.add(interaction);
            if (display.getBlock().getMaterial() == Material.GRINDSTONE) {
                customBlockManager.spawnCannonDisplay(display, cannonList, interaction, entities);
            }
        });

        // Register the active ship
        ActiveShip newShip = addActiveShip(standId, stand, entities, interactions, helmHeight);
        for (Interaction interaction : cannonList) {
            newShip.addCannon(interaction);
        }
        nearestPlayer.sendMessage(Component.text("Ship deployed!").color(NamedTextColor.GREEN));
    }

    private static @NotNull Vector getRotatedVector(SavedBlock sb, BlockFace facing) {
        double dx = sb.dx;
        double dy = sb.dy - 0.25;
        double dz = sb.dz;

        Vector rotatedOffset = new Vector();

        switch (facing) {
            case SOUTH: // No rotation needed
                rotatedOffset.setX(dx);
                rotatedOffset.setZ(dz);
                break;
            case EAST: // Rotate 90° clockwise
                rotatedOffset.setX(-dz);
                rotatedOffset.setZ(dx);
                break;
            case  NORTH: // Rotate 180°
                rotatedOffset.setX(-dx);
                rotatedOffset.setZ(-dz);
                break;
            case WEST: // Rotate 90° counter-clockwise
                rotatedOffset.setX(dz);
                rotatedOffset.setZ(-dx);
                break;
            default: // If unknown, default to NORTH
                rotatedOffset.setX(dx);
                rotatedOffset.setZ(dz);
                break;
        }

        rotatedOffset.setY(dy);
        return rotatedOffset.subtract(new Vector(0.5f, 0f, 0f));
    }

    public void spawnShip(UUID shipId, Player player, Location targetLoc) {
        plugin.getLogger().info("The dock is facing: " + StructurePlacementUtil.getCardinalFacing(targetLoc));
        List<SavedBlock> saved = getShipStructure(shipId);
        if (saved == null || saved.isEmpty()) {
            player.sendMessage(Component.text("No saved ship found.").color(NamedTextColor.RED));
            plugin.getLogger().warning("Error trying to place ship. No ship found for id: " + shipId);
            return;
        }
        SavedBlock helm = null;
        for (Iterator<SavedBlock> it = saved.iterator(); it.hasNext();) {
            SavedBlock value = it.next();
            if (value.getBlockData().getMaterial() == Material.OAK_FENCE && value.isCustomBlock) {
                helm = value;
            }
        }
        if (helm == null) return;
        Location baseLoc = targetLoc.clone().add(0,helm.dy-1,0);

        ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(adjustLocationToShipSize(baseLoc, saved), EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setMarker(false);
        stand.setGravity(true);
        stand.setInvulnerable(true);
        stand.customName(Component.text("ShipController"));
        stand.setCustomNameVisible(false);
        spawnShipEntitiesFromStructure(baseLoc, saved, stand, player);
    }

    private Location adjustLocationToShipSize(Location baseLoc, List<SavedBlock> saved) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (SavedBlock sb : saved) {
            int blockX, blockZ;
            switch (BlockFace.valueOf(sb.facing)) {
                case EAST: // Rotate 90° clockwise
                    blockX = -sb.dz;
                    blockZ = sb.dx;
                    break;
                case  NORTH: // Rotate 180°
                    blockX = -sb.dx;
                    blockZ = -sb.dz;
                    break;
                case WEST: // Rotate 90° counter-clockwise
                    blockX = sb.dz;
                    blockZ = -sb.dx;
                    break;
                default: // If unknown, default to SOUTH
                    blockX = sb.dx;
                    blockZ = sb.dz;
                    break;
            }
            if (blockX < minX) minX = blockX;
            if (blockX > maxX) maxX = blockX;
            if (blockZ < minZ) minZ = blockZ;
            if (blockZ > maxZ) maxZ = blockZ;
        }
        return StructurePlacementUtil.offsetByFacing(baseLoc, -minX, 0, -minZ, StructurePlacementUtil.getCardinalFacing(baseLoc));
    }

    public Location getPortForDisplayId(ItemDisplay nearestDisplay) {
        return portLocations.get(nearestDisplay.getUniqueId());
    }

    public void clearBlocksInShipyard(Location shipyard, int structWidth, int structLength, int structHeight) {
        World world = shipyard.getWorld();

        for (int x = -1; x < structWidth-1; x++) {
            for (int z = 0; z < structLength; z++) {
                for (int y = 0; y < structHeight; y++) {
                    boolean isCorner =
                            (x == -1 || x == structWidth - 2) &&
                                    (z == 0 || z == structLength - 1);
                    if (isCorner) continue;

                    Location loc = StructurePlacementUtil.offsetByFacing(shipyard, x, y, z, StructurePlacementUtil.getCardinalFacing(shipyard));
                    world.getBlockAt(loc).setType(Material.AIR);
                    UUID displayId = customBlockManager.removeCustomItemDisplay(loc);
                    if (displayId != null) {
                        Objects.requireNonNull(Bukkit.getEntity(displayId)).remove();
                    }
                }
            }
        }

        world.getBlockAt(shipyard).setType(Material.STONE_BUTTON);
    }
}
