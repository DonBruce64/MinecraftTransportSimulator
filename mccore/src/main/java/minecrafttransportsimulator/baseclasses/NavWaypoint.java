package minecrafttransportsimulator.baseclasses;
import java.util.Collections;

import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

import java.util.HashMap;
import java.util.Map;

public class NavWaypoint {
    private static final String WAYPOINT_LISTING_KEY = "waypoints";
    private static final Map<AWrapperWorld, Map<String, NavWaypoint>> cachedWaypointMaps = new HashMap<>();

    public final String name;
    public final double targetSpeed;
    public final double bearing;
    public final Point3D position;


    public static Map<String, NavWaypoint> getAllWaypointsFromWorld(AWrapperWorld world) {
        if (!cachedWaypointMaps.containsKey(world)) {
            IWrapperNBT waypointListing = world.getData(WAYPOINT_LISTING_KEY);
            if (waypointListing != null) {
                Map<String, NavWaypoint> waypointMap = new HashMap<>();
                for (String waypointName : waypointListing.getAllNames()) {
                    waypointMap.put(waypointName, new NavWaypoint(waypointListing.getData(waypointName)));
                }
                cachedWaypointMaps.put(world, waypointMap);
            } else {
                return Collections.emptyMap();
            }
        }

        return cachedWaypointMaps.get(world);
    }

    public static NavWaypoint getByNameFromWorld(AWrapperWorld world, String name) {
        if (!cachedWaypointMaps.containsKey(world)) {
            IWrapperNBT waypointListing = world.getData(WAYPOINT_LISTING_KEY);
            if (waypointListing != null) {
                Map<String, NavWaypoint> waypointMap = new HashMap<>();
                for (String waypointName : waypointListing.getAllNames()) {
                    waypointMap.put(waypointName, new NavWaypoint(waypointListing.getData(waypointName)));
                }
                cachedWaypointMaps.put(world, waypointMap);
            } else {
                return null;
            }
        }

        return cachedWaypointMaps.get(world).get(name);
    }

    public static void removeFromWorld(AWrapperWorld world, String name) {
        if (name != null) {
            if (cachedWaypointMaps.containsKey(world)) {
                cachedWaypointMaps.get(world).remove(name);
            }

            IWrapperNBT waypointListing = world.getData(WAYPOINT_LISTING_KEY);
            if (waypointListing != null) {
                waypointListing.deleteEntry(name);
                world.setData(WAYPOINT_LISTING_KEY, waypointListing);
            }
        }
    }

    private NavWaypoint(IWrapperNBT data) {
        this.name = data.getString("name");
        this.targetSpeed = data.getDouble("targetSpeed");
        this.bearing = data.getDouble("bearing");
        this.position = data.getPoint3dCompact("location");
    }

    public NavWaypoint(AWrapperWorld world, String name, double targetspeed, double bearing, Point3D position) {
        this.name = name;
        this.targetSpeed = targetspeed;
        this.bearing = bearing;
        this.position = position;
        IWrapperNBT waypointListing = world.getData(WAYPOINT_LISTING_KEY);
        if (waypointListing == null) {
            waypointListing = InterfaceManager.coreInterface.getNewNBTWrapper();
        }
        waypointListing.setData(name, save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        world.setData(WAYPOINT_LISTING_KEY, waypointListing);
        cachedWaypointMaps.remove(world);

    }

    public double getBearingDelta(AEntityB_Existing entity) {
        double delta = Math.toDegrees(Math.atan2(entity.position.x - position.x, entity.position.z - position.z)) + bearing + 180;
        while (delta < -180)
            delta += 360;
        while (delta > 180)
            delta -= 360;
        return delta;
    }

    public IWrapperNBT save(IWrapperNBT data) {
        data.setString("name", name);
        data.setDouble("targetSpeed", targetSpeed);
        data.setDouble("bearing", bearing);
        data.setPoint3dCompact("location", position);
        return data;
    }
}