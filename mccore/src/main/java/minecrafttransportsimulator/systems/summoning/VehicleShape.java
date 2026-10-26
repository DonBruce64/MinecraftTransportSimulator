package minecrafttransportsimulator.systems.summoning;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup;
import minecrafttransportsimulator.jsondefs.JSONCollisionBox;

/**
 * Rough local-space size of a vehicle, from its collision boxes and parts.
 * Used to check the vehicle fits where we path, teleport or land it.
 */
final class VehicleShape {
    /**Half the vehicle's width (local X), in blocks.**/
    final double halfWidth;
    /**Half the vehicle's length (local Z), in blocks.**/
    final double halfLength;
    /**Lowest point below the vehicle origin (negative), usually the bottom of the wheels.**/
    final double bottom;
    /**Highest point above the vehicle origin.**/
    final double top;
    /**Furthest point forwards of the vehicle origin.**/
    final double front;

    private VehicleShape(double halfWidth, double halfLength, double bottom, double top, double front) {
        this.halfWidth = halfWidth;
        this.halfLength = halfLength;
        this.bottom = bottom;
        this.top = top;
        this.front = front;
    }

    /**Total height from the bottom of the wheels to the top of the body.**/
    double height() {
        return top - bottom;
    }

    /**Radius of a circle the vehicle fits in at any yaw.**/
    double radius() {
        return Math.sqrt(halfWidth * halfWidth + halfLength * halfLength);
    }

    /**How far above the ground the vehicle origin sits when parked.**/
    double groundOffset() {
        return -bottom + 0.1;
    }

    static VehicleShape of(EntityVehicleF_Physics vehicle) {
        Bounds bounds = new Bounds();
        if (vehicle.definition.collisionGroups != null) {
            for (JSONCollisionGroup group : vehicle.definition.collisionGroups) {
                for (JSONCollisionBox box : group.collisions) {
                    bounds.include(box.pos, box.width / 2D, box.height / 2D);
                }
            }
        }
        for (APart part : vehicle.allParts) {
            if (part.entityOn == vehicle) {
                bounds.include(part.placementDefinition.pos, part.getWidth() / 2D, part.getHeight() / 2D);
            }
        }
        double halfWidth = Math.max(0.5, Math.max(-bounds.minX, bounds.maxX));
        double halfLength = Math.max(0.5, (bounds.maxZ - bounds.minZ) / 2D);
        return new VehicleShape(halfWidth, halfLength, Math.min(-0.5, bounds.minY), Math.max(1.0, bounds.maxY), Math.max(0.5, bounds.maxZ));
    }

    private static class Bounds {
        double minX, maxX, minY, maxY, minZ, maxZ;

        void include(Point3D pos, double horizontalRadius, double verticalRadius) {
            minX = Math.min(minX, pos.x - horizontalRadius);
            maxX = Math.max(maxX, pos.x + horizontalRadius);
            minY = Math.min(minY, pos.y - verticalRadius);
            maxY = Math.max(maxY, pos.y + verticalRadius);
            minZ = Math.min(minZ, pos.z - horizontalRadius);
            maxZ = Math.max(maxZ, pos.z + horizontalRadius);
        }
    }
}
