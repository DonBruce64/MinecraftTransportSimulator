package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;

/**
 * used for updating waypoint data from client.
 */
public class NavWaypointUpdater {
    public NavWaypoint currentWaypoint;

    public NavWaypointUpdater(NavWaypoint currentWaypoint) {
        this.currentWaypoint = currentWaypoint;
    }

    private void destroyFromWorld(AWrapperWorld world) {
        if (currentWaypoint != null) {
            NavWaypoint.removeFromWorld(world, currentWaypoint.index);
        }
    }
    public void updateStateFromWorld(AWrapperWorld world, String waypointIndex, String waypointName, String targetSpeedStr, String bearingStr, String xStr, String yStr, String zStr, String isDeleted ) {
        boolean Deleted = Boolean.parseBoolean(isDeleted);

        if(Deleted == true){
            destroyFromWorld(world);
        }else{
            try {
                if (waypointIndex == null || waypointIndex.trim().isEmpty()) {
                    throw new IllegalArgumentException("Waypoint index cannot be empty.");
                }

                String name = waypointName;

                double targetSpeed = Double.parseDouble(targetSpeedStr);

                double bearing = Double.parseDouble(bearingStr);

                double x = Double.parseDouble(xStr);
                double y = Double.parseDouble(yStr);
                double z = Double.parseDouble(zStr);

                if (currentWaypoint != null) {
                    NavWaypoint.removeFromWorld(world, currentWaypoint.index);
                    currentWaypoint = null;
                }
                currentWaypoint = new NavWaypoint(world,waypointIndex,waypointName,targetSpeed,bearing,new Point3D(x, y, z));

            } catch (NumberFormatException e) {
//            System.err.println("Invalid input: Expected numerical values for targetSpeed, bearing, and position.");
            } catch (IllegalArgumentException e) {
//            System.err.println("Invalid input: " + e.getMessage());
            } catch (Exception e) {
//            System.err.println("An unexpected error occurred: " + e.getMessage());
            }
        }

    }

    private void destroyFromVehicle(AEntityD_Definable entity, IWrapperNBT data) {
        if (currentWaypoint != null) {
            NavWaypoint.removeFromVehicle(entity, currentWaypoint.index,data);
        }
    }
    public void updateStateFromVehicle(AEntityD_Definable entity,IWrapperNBT data ,String waypointIndex, String waypointName, String targetSpeedStr, String bearingStr, String xStr, String yStr, String zStr, String isDeleted ) {
        boolean Deleted = Boolean.parseBoolean(isDeleted);

        if(Deleted == true){
            destroyFromVehicle(entity, data);
        }else{
            try {
                if (waypointIndex == null || waypointIndex.trim().isEmpty()) {
                    throw new IllegalArgumentException("Waypoint index cannot be empty.");
                }

                String name = waypointName;

                double targetSpeed = Double.parseDouble(targetSpeedStr);

                double bearing = Double.parseDouble(bearingStr);

                double x = Double.parseDouble(xStr);
                double y = Double.parseDouble(yStr);
                double z = Double.parseDouble(zStr);

                if (currentWaypoint != null) {
                    NavWaypoint.removeFromVehicle(entity, currentWaypoint.index, data);
                    currentWaypoint = null;
                }
                currentWaypoint = new NavWaypoint(entity,waypointIndex,waypointName,targetSpeed,bearing,new Point3D(x, y, z),data);

            } catch (NumberFormatException e) {
//            System.err.println("Invalid input: Expected numerical values for targetSpeed, bearing, and position.");
            } catch (IllegalArgumentException e) {
//            System.err.println("Invalid input: " + e.getMessage());
            } catch (Exception e) {
//            System.err.println("An unexpected error occurred: " + e.getMessage());
            }
        }

    }
}
