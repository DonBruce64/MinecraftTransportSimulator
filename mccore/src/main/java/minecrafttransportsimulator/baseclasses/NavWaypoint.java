package minecrafttransportsimulator.baseclasses;
import java.util.*;

import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * waypoint class
 */

//structure:{waypoints:{index:value,name:value,targetSpeed:value,bearing:value,position:value}}

public class NavWaypoint {
    public static final String WAYPOINT_LISTING_KEY = "waypoints";
    private static final Map<AWrapperWorld, Map<String, NavWaypoint>> cachedWaypointMaps = new HashMap<>();
    private static final Map<AEntityD_Definable, Map<String, NavWaypoint>> v_cachedWaypointMaps = new HashMap<>();

    //use an editable value as key
    public final String index;
    //information
    public final String name;
    public final double targetSpeed;
    public final double bearing;
    public final Point3D position;



    //Global Operation
    public static Map<String, NavWaypoint> getAllWaypointsFromWorld(AWrapperWorld world) {
        if (!cachedWaypointMaps.containsKey(world)) {
            IWrapperNBT waypointListing = world.getData(WAYPOINT_LISTING_KEY);
            if (waypointListing != null) {
                Map<String, NavWaypoint> waypointMap = new HashMap<>();
                for (String waypointIndex : waypointListing.getAllNames()) {
                    waypointMap.put(waypointIndex, new NavWaypoint(waypointListing.getData(waypointIndex)));
                }
                cachedWaypointMaps.put(world, waypointMap);
            } else {
                return Collections.emptyMap();
            }
        }

        return cachedWaypointMaps.get(world);
    }


    public static NavWaypoint getByIndexFromWorld(AWrapperWorld world, String index) {
        if (!cachedWaypointMaps.containsKey(world)) {
            IWrapperNBT waypointListing = world.getData(WAYPOINT_LISTING_KEY);
            if (waypointListing != null) {
                Map<String, NavWaypoint> waypointMap = new HashMap<>();
                for (String waypointIndex : waypointListing.getAllNames()) {
                    waypointMap.put(waypointIndex, new NavWaypoint(waypointListing.getData(waypointIndex)));
                }
                cachedWaypointMaps.put(world, waypointMap);
            } else {
                return null;
            }
        }

        return cachedWaypointMaps.get(world).get(index);
    }

    public static void removeFromWorld(AWrapperWorld world, String index) {
        if (index != null) {
            if (cachedWaypointMaps.containsKey(world)) {
                cachedWaypointMaps.get(world).remove(index);
            }

            IWrapperNBT waypointListing = world.getData(WAYPOINT_LISTING_KEY);
            if (waypointListing != null) {
                waypointListing.deleteEntry(index);
                world.setData(WAYPOINT_LISTING_KEY, waypointListing);
            }
        }
    }


    public NavWaypoint(AWrapperWorld world, String index, String name, double targetspeed, double bearing, Point3D position) {
        this.index = index;
        this.name = name;
        this.targetSpeed = targetspeed;
        this.bearing = bearing;
        this.position = position;
        IWrapperNBT waypointListing = world.getData(WAYPOINT_LISTING_KEY);
        if (waypointListing == null) {
            waypointListing = InterfaceManager.coreInterface.getNewNBTWrapper();
        }
        waypointListing.setData(index, save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        world.setData(WAYPOINT_LISTING_KEY, waypointListing);
        cachedWaypointMaps.remove(world);
    }


    //Vehicle Operation

    public NavWaypoint(AEntityD_Definable<?> entity, String index, String name, double targetspeed, double bearing, Point3D position, IWrapperNBT data) {
        this.index = index;
        this.name = name;
        this.targetSpeed = targetspeed;
        this.bearing = bearing;
        this.position = position;
        IWrapperNBT waypointListing = data.getData(WAYPOINT_LISTING_KEY);
        if (waypointListing == null) {
            waypointListing = InterfaceManager.coreInterface.getNewNBTWrapper();
        }
        waypointListing.setData(index, save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        data.setData(WAYPOINT_LISTING_KEY, waypointListing);
        v_cachedWaypointMaps.remove(entity);
    }

    public static NavWaypoint getByIndexFromVehicle(AEntityD_Definable entity, String index, IWrapperNBT data) {
        //check data
        if(data == null)return null;

        if (!v_cachedWaypointMaps.containsKey(entity)) {
            IWrapperNBT waypointListing = data.getData(WAYPOINT_LISTING_KEY);
            if (waypointListing != null) {
                Map<String, NavWaypoint> waypointMap = new HashMap<>();
                for (String waypointIndex : waypointListing.getAllNames()) {
                    waypointMap.put(waypointIndex, new NavWaypoint(waypointListing.getData(waypointIndex)));
                }
                v_cachedWaypointMaps.put(entity, waypointMap);
            } else {
                return null;
            }
        }

        //check if this index is existed
        if(v_cachedWaypointMaps.get(entity)!=null)return v_cachedWaypointMaps.get(entity).get(index);
        else return null;
    }
    public static Map<String, NavWaypoint> getAllWaypointsFromVehicle(AEntityD_Definable entity, IWrapperNBT data) {
        //check data
        if(data == null)return null;

        if (!v_cachedWaypointMaps.containsKey(entity)) {
            IWrapperNBT waypointListing = data.getData(WAYPOINT_LISTING_KEY);
            if (waypointListing != null) {
                Map<String, NavWaypoint> waypointMap = new HashMap<>();
                for (String waypointIndex : waypointListing.getAllNames()) {
                    waypointMap.put(waypointIndex, new NavWaypoint(waypointListing.getData(waypointIndex)));
                }
                v_cachedWaypointMaps.put(entity, waypointMap);
            } else {
                return null;
            }
        }

        return v_cachedWaypointMaps.get(entity);
    }
    public static void removeFromVehicle(AEntityD_Definable entity, String index, IWrapperNBT data) {
        if (index != null) {
            if (v_cachedWaypointMaps.containsKey(entity)) {
                v_cachedWaypointMaps.get(entity).remove(index);
            }

            IWrapperNBT waypointListing = data.getData(WAYPOINT_LISTING_KEY);
            if (waypointListing != null) {
                waypointListing.deleteEntry(index);
                data.setData(WAYPOINT_LISTING_KEY, waypointListing);
            }
        }
    }



    //Universal Operation

    //internal init
    private NavWaypoint(IWrapperNBT data) {
        this.index = data.getString("index");
        this.name = data.getString("name");
        this.targetSpeed = data.getDouble("targetSpeed");
        this.bearing = data.getDouble("bearing");
        this.position = data.getPoint3dCompact("location");
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
        data.setString("index",index);
        data.setString("name", name);
        data.setDouble("targetSpeed", targetSpeed);
        data.setDouble("bearing", bearing);
        data.setPoint3dCompact("location", position);
        return data;
    }

    public static void sortWaypointListByIndex(List<NavWaypoint> globalWaypointList) {
        Collections.sort(globalWaypointList, new Comparator<NavWaypoint>() {
            @Override
            public int compare(NavWaypoint wp1, NavWaypoint wp2) {
                try {
                    int index1 = Integer.parseInt(wp1.index);
                    int index2 = Integer.parseInt(wp2.index);
                    return Integer.compare(index1, index2);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        });
    }
}