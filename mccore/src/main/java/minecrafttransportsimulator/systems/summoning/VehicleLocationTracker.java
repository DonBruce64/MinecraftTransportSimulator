package minecrafttransportsimulator.systems.summoning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Remembers where every vehicle was last seen, per world, so phones can find vehicles
 * whose chunks aren't loaded.  Server only.
 * <p>
 * Positions are kept in memory and saved every so often to a file of their own next to the world's
 * MTS data.  They're never sent to clients, so players can't find out where everyone's vehicles are.
 * Vehicles are forgotten when they're picked up, or when a call finds they're no longer where they were
 * last seen, so the file doesn't fill up with vehicles that are long gone.
 */
public final class VehicleLocationTracker {
    private static final String FILE_NAME = "mtsvehiclelocations.txt";
    private static final int TRACK_INTERVAL = 20;
    private static final int SAVE_INTERVAL = 600;

    private static final Map<AWrapperWorld, WorldLocations> worlds = new HashMap<>();

    private VehicleLocationTracker() {
    }

    /**Called every vehicle tick on the server.**/
    static void onVehicleTick(EntityVehicleF_Physics vehicle) {
        if (vehicle.ticksExisted % TRACK_INTERVAL == 0) {
            update(vehicle);
        }
    }

    /**Records the vehicle's current position now.**/
    static void update(EntityVehicleF_Physics vehicle) {
        WorldLocations locations = get(vehicle.world);
        Point3D last = locations.positions.get(vehicle.uniqueUUID);
        if (last == null || !last.isDistanceToCloserThan(vehicle.position, 1)) {
            locations.positions.put(vehicle.uniqueUUID, vehicle.position.copy());
            locations.dirty = true;
        }
    }

    /**Forgets the vehicle, for when it's been removed from the world for good, like being picked up.**/
    public static void forget(EntityVehicleF_Physics vehicle) {
        if (!vehicle.world.isClient()) {
            forget(vehicle.world, vehicle.uniqueUUID);
        }
    }

    /**Forgets the vehicle in this world.**/
    static void forget(AWrapperWorld world, UUID vehicleID) {
        WorldLocations locations = get(world);
        if (locations.positions.remove(vehicleID) != null) {
            locations.dirty = true;
        }
    }

    /**Where the vehicle was last seen in this world, or null if it never was.**/
    static Point3D getLastPosition(AWrapperWorld world, UUID vehicleID) {
        return get(world).positions.get(vehicleID);
    }

    /**Called every server tick for the world.  Saves changed positions now and then.**/
    static void tick(AWrapperWorld world) {
        WorldLocations locations = worlds.get(world);
        if (locations != null && --locations.ticksUntilSave <= 0) {
            locations.ticksUntilSave = SAVE_INTERVAL;
            save(world, locations);
        }
    }

    /**Saves and forgets the world, for when it unloads.  It's re-read if it loads again.**/
    static void onWorldUnload(AWrapperWorld world) {
        WorldLocations locations = worlds.remove(world);
        if (locations != null) {
            save(world, locations);
        }
    }

    private static File getFile(AWrapperWorld world) {
        return new File(world.getDataFile().getParentFile(), FILE_NAME);
    }

    /**
     * Writes the positions if they've changed, one vehicle a line as "uuid x y z".  Writes to a
     * temporary file first and then swaps it in, so a crash mid-save can't leave a half-written file.
     */
    private static void save(AWrapperWorld world, WorldLocations locations) {
        if (!locations.dirty) {
            return;
        }
        File file = getFile(world);
        File tempFile = new File(file.getParentFile(), FILE_NAME + ".tmp");
        try {
            file.getParentFile().mkdirs();
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8)) {
                for (Entry<UUID, Point3D> entry : locations.positions.entrySet()) {
                    Point3D position = entry.getValue();
                    writer.write(entry.getKey() + " " + position.x + " " + position.y + " " + position.z);
                    writer.newLine();
                }
            }
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            locations.dirty = false;
        } catch (IOException e) {
            InterfaceManager.coreInterface.logError("Could not save vehicle locations to " + file + ": " + e.getMessage());
        }
    }

    /**Reads the world's positions the first time they're needed.**/
    private static WorldLocations get(AWrapperWorld world) {
        return worlds.computeIfAbsent(world, w -> {
            WorldLocations locations = new WorldLocations();
            File file = getFile(w);
            if (file.exists()) {
                try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] values = line.trim().split(" ");
                        if (values.length >= 4) {
                            try {
                                locations.positions.put(UUID.fromString(values[0]), new Point3D(Double.parseDouble(values[1]), Double.parseDouble(values[2]), Double.parseDouble(values[3])));
                            } catch (IllegalArgumentException e) {
                                //Corrupt line.  Skip it.
                            }
                        }
                    }
                } catch (IOException e) {
                    InterfaceManager.coreInterface.logError("Could not load vehicle locations from " + file + ": " + e.getMessage());
                }
            }
            return locations;
        });
    }

    private static class WorldLocations {
        final Map<UUID, Point3D> positions = new HashMap<>();
        int ticksUntilSave = SAVE_INTERVAL;
        boolean dirty;
    }
}
