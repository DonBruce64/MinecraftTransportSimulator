package minecrafttransportsimulator.systems.summoning;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Remembers where every vehicle was last seen, per world, so phones can find vehicles
 * whose chunks aren't loaded.  Positions are kept in memory and saved to the world's data
 * every so often, as each save also sends the data to every client.  Server only.
 */
public final class VehicleLocationTracker {
    private static final String DATA_NAME = "vehicle_locations";
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

    private static void save(AWrapperWorld world, WorldLocations locations) {
        if (locations.dirty) {
            IWrapperNBT data = InterfaceManager.coreInterface.getNewNBTWrapper();
            locations.positions.forEach((id, position) -> data.setPoint3d(id.toString(), position));
            world.setData(DATA_NAME, data);
            locations.dirty = false;
        }
    }

    private static WorldLocations get(AWrapperWorld world) {
        return worlds.computeIfAbsent(world, w -> {
            WorldLocations locations = new WorldLocations();
            IWrapperNBT data = w.getData(DATA_NAME);
            if (data != null) {
                for (String name : data.getAllNames()) {
                    try {
                        locations.positions.put(UUID.fromString(name), data.getPoint3d(name));
                    } catch (IllegalArgumentException e) {
                        //Not a vehicle entry.  Skip it.
                    }
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
