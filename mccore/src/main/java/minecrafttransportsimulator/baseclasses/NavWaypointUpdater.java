package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.mcinterface.AWrapperWorld;
/**
 * used for updating waypoint data from client.
 */
public class NavWaypointUpdater {
    public NavWaypoint currentWaypoint;

    public NavWaypointUpdater(NavWaypoint currentWaypoint) {
        this.currentWaypoint = currentWaypoint;
    }

    public void destroy(AWrapperWorld world) {
        if (currentWaypoint != null) {
            NavWaypoint.removeFromWorld(world, currentWaypoint.index);
        }
    }
    public void updateState(AWrapperWorld world, String waypointIndex, String waypointName, String targetSpeedStr, String bearingStr, String xStr, String yStr, String zStr, String isDeleted ) {
        boolean Deleted = Boolean.parseBoolean(isDeleted);

        if(Deleted == true){
            destroy(world);
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
}
