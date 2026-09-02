package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;

/**
 * Basic bounding box.  This class is mutable and allows for quick setting of values
 * without the need to make a new instance every time.  Also is based on a center point and
 * height and width parameters rather than min/max, though such parameters are calculated to be
 * used in bounds checks.  Note that rather than width and height we use radius here.  The idea
 * being that addition is quicker than multiplication, and most of the time we're doing checks
 * for things a specific distance away rather than within a specific width, height, and depth.
 * For reference, depth is in the Z-direction, while width is in the X-direction.
 * <br><br>
 * Of note is how we set the center points.  The first point passed-in is the boxes' local
 * center point.  This should NEVER be modified, as it's designed to never change and always be relative
 * to the center of the object that owns this box.  The second global parameter represents the boxes'
 * actual center point in the world, when all appropriate translations/rotations have been performed.
 * Most, if not all, updates to boxes on an object will simply require modifying this second parameter.
 *
 * @author don_bruce
 */
public class BoundingBox {
    private static final double HITBOX_CLAMP = 0.015625;
    private static final double OBB_EPSILON = 1.0E-7D;
    public final Point3D localCenter;
    public final Point3D globalCenter;
    public final RotationMatrix orientation;
    public final Point3D currentCollisionDepth;
    public final List<Point3D> collidingBlockPositions = new ArrayList<>();
    private RenderableData wireframeRenderable;
    private RenderableData holographicRenderable;
    private final Point3D tempGlobalCenter;

    public double widthRadius;
    public double heightRadius;
    public double depthRadius;
    public final boolean collidesWithLiquids;
    public final JSONCollisionGroup groupDef;
    public final JSONCollisionBox definition;
    public final Set<CollisionType> collisionTypes;

    private static final Point3D helperPoint = new Point3D();

    /**
     * Simplest constructor.  Used for simple bounds.
     **/
    public BoundingBox(Point3D center, double radius) {
        this(new Point3D(), center, radius, radius, radius, false, null, null, null);
    }

    /**
     * Simple constructor.  Used for blocks, bounds checks, or other things that don't need local/global positional differences.
     **/
    public BoundingBox(Point3D center, double widthRadius, double heightRadius, double depthRadius) {
        this(new Point3D(), center, widthRadius, heightRadius, depthRadius, false, null, null, null);
    }

    /**
     * Like the other simple constructor, but with a parameter for collision type.
     **/
    public BoundingBox(Point3D center, double widthRadius, double heightRadius, double depthRadius, Set<CollisionType> collisionTypes) {
        this(new Point3D(), center, widthRadius, heightRadius, depthRadius, false, null, null, collisionTypes);
    }

    /**
     * Complex constructor.  Used for things that have local and global positions.  These can also collide with liquid blocks.
     **/
    public BoundingBox(Point3D localCenter, Point3D globalCenter, double widthRadius, double heightRadius, double depthRadius, boolean collidesWithLiquids, Set<CollisionType> collisionTypes) {
        this(localCenter, globalCenter, widthRadius, heightRadius, depthRadius, collidesWithLiquids, null, null, collisionTypes);
    }

    /**
     * JSON constructor.  Used for boxes that are created from JSON and need extended properties.
     **/
    public BoundingBox(JSONCollisionBox definition, JSONCollisionGroup groupDef) {
        this(definition.pos, definition.pos.copy(), definition.width / 2D, definition.height / 2D, (definition.length != 0 ? definition.length : definition.width) / 2D, definition.collidesWithLiquids, definition, groupDef, groupDef.collisionTypes);
    }

    /**
     * Vector constructor.  Creates a box for a vector.  Used mainly in raytracing applications for pre-calculation.
     **/
    public BoundingBox(Point3D start, Point3D end) {
        this(new Point3D(), 0, 0, 0);
        globalCenter.set(end).subtract(start).scale(0.5);
        widthRadius = Math.abs(globalCenter.x);
        heightRadius = Math.abs(globalCenter.y);
        depthRadius = Math.abs(globalCenter.z);
        globalCenter.add(start);
    }

    /**
     * Master constructor.  Used for main creation.
     **/
    private BoundingBox(Point3D localCenter, Point3D globalCenter, double widthRadius, double heightRadius, double depthRadius, boolean collidesWithLiquids, JSONCollisionBox definition, JSONCollisionGroup groupDef, Set<CollisionType> collisionTypes) {
        this.localCenter = localCenter;
        this.globalCenter = globalCenter;
        this.orientation = new RotationMatrix();
        this.tempGlobalCenter = globalCenter.copy();
        this.currentCollisionDepth = new Point3D();
        this.widthRadius = widthRadius;
        this.heightRadius = heightRadius;
        this.depthRadius = depthRadius;
        this.collidesWithLiquids = collidesWithLiquids;
        this.groupDef = groupDef;
        this.definition = definition;
        this.collisionTypes = collisionTypes;
    }

    @Override
    public String toString() {
        return "LocalCenter:" + localCenter.toString() + " GlobalCenter:" + globalCenter.toString() + " Width:" + widthRadius + " Height:" + heightRadius + " Depth:" + depthRadius;
    }

    /**
     * Populates the collidingBlocks list with all currently-colliding blocks.
     * Note that the passed-in offset is only applied for this check,  and is reverted after this call.
     * If blocks collided with this box after this method, true is returned.
     */
    public boolean updateCollisions(AWrapperWorld world, Point3D offset, boolean ignoreIfGreater) {
        tempGlobalCenter.set(globalCenter);
        globalCenter.add(offset);
        world.updateBoundingBoxCollisions(this, offset, ignoreIfGreater);
        globalCenter.set(tempGlobalCenter);
        return !collidingBlockPositions.isEmpty();
    }

    /**
     * Sets the global center of this box to the position of the passed-in entity, rotated by the
     * entity's rotation and offset by the local center, or the passed-in offset if it is non-null.
     * Mostly used for updating hitboxes that rotate with the entity.  Rotation is done using the fine
     * Point3d rotation to allow for better interaction while standing on entities.
     */
    public void updateToEntity(AEntityD_Definable<?> entity, Point3D optionalOffset) {
        updateToEntity(entity, optionalOffset, null);
    }

    public void updateToEntity(AEntityD_Definable<?> entity, Point3D optionalOffset, RotationMatrix optionalRotation) {
        if (optionalOffset != null) {
            globalCenter.set(optionalOffset);
        } else {
            globalCenter.set(localCenter);
        }
        globalCenter.multiply(entity.scale).rotate(entity.orientation).add(entity.position);
        orientation.set(entity.orientation);
        if (optionalRotation != null) {
            orientation.multiply(optionalRotation);
        }
        if (definition != null && definition.rot != null) {
            orientation.multiply(definition.rot);
        }
        if (groupDef != null && (groupDef.collisionTypes.contains(CollisionType.ENTITY) || groupDef.collisionTypes.contains(CollisionType.VEHICLE))) {
            //Need to round box to prevent floating-point errors for player and entity collision.
            globalCenter.x = ((int) (globalCenter.x / HITBOX_CLAMP)) * HITBOX_CLAMP;
            globalCenter.y = ((int) (globalCenter.y / HITBOX_CLAMP)) * HITBOX_CLAMP;
            globalCenter.z = ((int) (globalCenter.z / HITBOX_CLAMP)) * HITBOX_CLAMP;
        }
        if (definition != null) {
            widthRadius = entity.scale.x * definition.width / 2D;
            heightRadius = entity.scale.y * definition.height / 2D;
            depthRadius = entity.scale.z * (definition.length != 0 ? definition.length : definition.width) / 2D;
        }
    }

    /**
     * Returns true if this box should use OBB logic.  OBBs are only enabled for
     * collision groups whose types are all supported by the OBB implementation.
     */
    public boolean isOBB() {
        if (groupDef != null && groupDef.isOBB) {
            for (CollisionType type : groupDef.collisionTypes) {
                if (type != CollisionType.BULLET && type != CollisionType.ATTACK && type != CollisionType.VEHICLE && type != CollisionType.CLICK) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the world-axis X radius of this box.  For AABBs this is the raw
     * width radius; for OBBs it is the projection of the oriented box onto X.
     */
    public double getXRadius() {
        return isOBB() ? Math.abs(orientation.m00) * widthRadius + Math.abs(orientation.m01) * heightRadius + Math.abs(orientation.m02) * depthRadius : widthRadius;
    }

    /**
     * Returns the world-axis Y radius of this box.  For AABBs this is the raw
     * height radius; for OBBs it is the projection of the oriented box onto Y.
     */
    public double getYRadius() {
        return isOBB() ? Math.abs(orientation.m10) * widthRadius + Math.abs(orientation.m11) * heightRadius + Math.abs(orientation.m12) * depthRadius : heightRadius;
    }

    /**
     * Returns the world-axis Z radius of this box.  For AABBs this is the raw
     * depth radius; for OBBs it is the projection of the oriented box onto Z.
     */
    public double getZRadius() {
        return isOBB() ? Math.abs(orientation.m20) * widthRadius + Math.abs(orientation.m21) * heightRadius + Math.abs(orientation.m22) * depthRadius : depthRadius;
    }

    public double getMinX() {
        return globalCenter.x - getXRadius();
    }

    public double getMaxX() {
        return globalCenter.x + getXRadius();
    }

    public double getMinY() {
        return globalCenter.y - getYRadius();
    }

    public double getMaxY() {
        return globalCenter.y + getYRadius();
    }

    public double getMinZ() {
        return globalCenter.z - getZRadius();
    }

    public double getMaxZ() {
        return globalCenter.z + getZRadius();
    }

    /**
     * Returns true if the passed-in point is inside this box.
     * Note that this returns true for points on the border, to allow use to use in
     * in conjunction with hit-scanning code to find out which box got hit-scanned.
     */
    public boolean isPointInside(Point3D point, Point3D growthOffset) {
        if (isOBB()) {
            Point3D localPoint = point.copy().subtract(globalCenter).reOrigin(orientation);
            double growthX = growthOffset != null ? growthOffset.x : 0;
            double growthY = growthOffset != null ? growthOffset.y : 0;
            double growthZ = growthOffset != null ? growthOffset.z : 0;
            return localPoint.x >= -widthRadius - growthX && localPoint.x <= widthRadius + growthX && localPoint.y >= -heightRadius - growthY && localPoint.y <= heightRadius + growthY && localPoint.z >= -depthRadius - growthZ && localPoint.z <= depthRadius + growthZ;
        }
        if (growthOffset != null) {
            return globalCenter.x - widthRadius - growthOffset.x <= point.x && globalCenter.x + widthRadius + growthOffset.x >= point.x && globalCenter.y - heightRadius - growthOffset.y <= point.y && globalCenter.y + heightRadius + growthOffset.y >= point.y && globalCenter.z - depthRadius - growthOffset.z <= point.z && globalCenter.z + depthRadius + growthOffset.z >= point.z;
        } else {
            return globalCenter.x - widthRadius <= point.x && globalCenter.x + widthRadius >= point.x && globalCenter.y - heightRadius <= point.y && globalCenter.y + heightRadius >= point.y && globalCenter.z - depthRadius <= point.z && globalCenter.z + depthRadius >= point.z;
        }
    }

    /**
     * Returns true if the passed-in point is inside this box in the XZ plane, and is below this box.
     */
    public boolean isPointInsideAndBelow(Point3D point) {
        if (isOBB()) {
            Point3D localPoint = point.copy().subtract(globalCenter).reOrigin(orientation);
            return localPoint.x >= -widthRadius && localPoint.x <= widthRadius && localPoint.y <= heightRadius && localPoint.z >= -depthRadius && localPoint.z <= depthRadius;
        }
        return globalCenter.x - widthRadius <= point.x && globalCenter.x + widthRadius >= point.x && globalCenter.y + heightRadius > point.y && globalCenter.z - depthRadius <= point.z && globalCenter.z + depthRadius >= point.z;
    }

    /**
     * Returns true if the passed-in box intersects this box.
     */
    public boolean intersects(BoundingBox box) {
        if (isOBB() || box.isOBB()) {
            return intersectsOBB(box);
        }
        return globalCenter.x - widthRadius < box.globalCenter.x + box.widthRadius && globalCenter.x + widthRadius > box.globalCenter.x - box.widthRadius && globalCenter.y - heightRadius < box.globalCenter.y + box.heightRadius && globalCenter.y + heightRadius > box.globalCenter.y - box.heightRadius && globalCenter.z - depthRadius < box.globalCenter.z + box.depthRadius && globalCenter.z + depthRadius > box.globalCenter.z - box.depthRadius;
    }

    /**
     * Returns true if this box intersects the passed-in world-axis bounds.
     */
    public boolean intersects(double otherMinX, double otherMinY, double otherMinZ, double otherMaxX, double otherMaxY, double otherMaxZ) {
        if (isOBB()) {
            return intersects(new BoundingBox(new Point3D((otherMinX + otherMaxX) / 2D, (otherMinY + otherMaxY) / 2D, (otherMinZ + otherMaxZ) / 2D), (otherMaxX - otherMinX) / 2D, (otherMaxY - otherMinY) / 2D, (otherMaxZ - otherMinZ) / 2D));
        }
        return otherMaxX > globalCenter.x - widthRadius && otherMinX < globalCenter.x + widthRadius && otherMaxY > globalCenter.y - heightRadius && otherMinY < globalCenter.y + heightRadius && otherMaxZ > globalCenter.z - depthRadius && otherMinZ < globalCenter.z + depthRadius;
    }

    private boolean intersectsOBB(BoundingBox box) {
        double[] thisRadii = { widthRadius, heightRadius, depthRadius };
        double[] otherRadii = { box.widthRadius, box.heightRadius, box.depthRadius };
        double[][] rotation = new double[3][3];
        double[][] absRotation = new double[3][3];
        boolean thisOBB = isOBB();
        boolean otherOBB = box.isOBB();
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                rotation[i][j] = getAxisDot(this, i, thisOBB, box, j, otherOBB);
                absRotation[i][j] = Math.abs(rotation[i][j]) + OBB_EPSILON;
            }
        }

        Point3D centerDelta = box.globalCenter.copy().subtract(globalCenter);
        double[] translation = {
            getAxisDot(centerDelta, this, 0, thisOBB),
            getAxisDot(centerDelta, this, 1, thisOBB),
            getAxisDot(centerDelta, this, 2, thisOBB)
        };

        for (int i = 0; i < 3; ++i) {
            double otherRadius = otherRadii[0] * absRotation[i][0] + otherRadii[1] * absRotation[i][1] + otherRadii[2] * absRotation[i][2];
            if (Math.abs(translation[i]) > thisRadii[i] + otherRadius) {
                return false;
            }
        }

        for (int j = 0; j < 3; ++j) {
            double thisRadius = thisRadii[0] * absRotation[0][j] + thisRadii[1] * absRotation[1][j] + thisRadii[2] * absRotation[2][j];
            double distance = Math.abs(translation[0] * rotation[0][j] + translation[1] * rotation[1][j] + translation[2] * rotation[2][j]);
            if (distance > thisRadius + otherRadii[j]) {
                return false;
            }
        }

        for (int i = 0; i < 3; ++i) {
            int i1 = (i + 1) % 3;
            int i2 = (i + 2) % 3;
            for (int j = 0; j < 3; ++j) {
                int j1 = (j + 1) % 3;
                int j2 = (j + 2) % 3;
                double thisRadius = thisRadii[i1] * absRotation[i2][j] + thisRadii[i2] * absRotation[i1][j];
                double otherRadius = otherRadii[j1] * absRotation[i][j2] + otherRadii[j2] * absRotation[i][j1];
                double distance = Math.abs(translation[i2] * rotation[i1][j] - translation[i1] * rotation[i2][j]);
                if (distance > thisRadius + otherRadius) {
                    return false;
                }
            }
        }
        return true;
    }

    private static double getAxisDot(BoundingBox firstBox, int firstAxis, boolean firstOBB, BoundingBox secondBox, int secondAxis, boolean secondOBB) {
        return getAxisComponent(firstBox, firstAxis, firstOBB, 0) * getAxisComponent(secondBox, secondAxis, secondOBB, 0) + getAxisComponent(firstBox, firstAxis, firstOBB, 1) * getAxisComponent(secondBox, secondAxis, secondOBB, 1) + getAxisComponent(firstBox, firstAxis, firstOBB, 2) * getAxisComponent(secondBox, secondAxis, secondOBB, 2);
    }

    private static double getAxisDot(Point3D point, BoundingBox box, int axis, boolean boxOBB) {
        return point.x * getAxisComponent(box, axis, boxOBB, 0) + point.y * getAxisComponent(box, axis, boxOBB, 1) + point.z * getAxisComponent(box, axis, boxOBB, 2);
    }

    private static double getAxisComponent(BoundingBox box, int axis, boolean boxOBB, int component) {
        if (!boxOBB) {
            return axis == component ? 1D : 0D;
        }
        switch (axis) {
            case 0:
                return component == 0 ? box.orientation.m00 : component == 1 ? box.orientation.m10 : box.orientation.m20;
            case 1:
                return component == 0 ? box.orientation.m01 : component == 1 ? box.orientation.m11 : box.orientation.m21;
            default:
                return component == 0 ? box.orientation.m02 : component == 1 ? box.orientation.m12 : box.orientation.m22;
        }
    }

    /**
     * Returns true if the passed-in point intersects this box in the YZ-plane.
     */
    private boolean intersectsWithYZ(Point3D point) {
        return point.y >= globalCenter.y - heightRadius && point.y <= globalCenter.y + heightRadius && point.z >= globalCenter.z - depthRadius && point.z <= globalCenter.z + depthRadius;
    }

    /**
     * Returns true if the passed-in point intersects this box in the XZ-plane.
     */
    private boolean intersectsWithXZ(Point3D point) {
        return point.x >= globalCenter.x - widthRadius && point.x <= globalCenter.x + widthRadius && point.z >= globalCenter.z - depthRadius && point.z <= globalCenter.z + depthRadius;
    }

    /**
     * Returns true if the passed-in point intersects this box in the XY-plane.
     */
    private boolean intersectsWithXY(Point3D point) {
        return point.x >= globalCenter.x - widthRadius && point.x <= globalCenter.x + widthRadius && point.y >= globalCenter.y - heightRadius && point.y <= globalCenter.y + heightRadius;
    }

    /**
     * Returns the point between the start and end points that collides with this box,
     * or null if such a point does not exist.
     */
    private Point3D getXPlaneCollision(Point3D start, Point3D end, double xPoint) {
        Point3D collisionPoint = start.getIntermediateWithXValue(end, xPoint);
        return collisionPoint != null && this.intersectsWithYZ(collisionPoint) ? collisionPoint : null;
    }

    /**
     * Returns the point between the start and end points that collides with this box,
     * or null if such a point does not exist.
     */
    private Point3D getYPlaneCollision(Point3D start, Point3D end, double yPoint) {
        Point3D collisionPoint = start.getIntermediateWithYValue(end, yPoint);
        return collisionPoint != null && this.intersectsWithXZ(collisionPoint) ? collisionPoint : null;
    }

    /**
     * Returns the point between the start and end points that collides with this box,
     * or null if such a point does not exist.
     */
    private Point3D getZPlaneCollision(Point3D start, Point3D end, double zPoint) {
        Point3D collisionPoint = start.getIntermediateWithZValue(end, zPoint);
        return collisionPoint != null && this.intersectsWithXY(collisionPoint) ? collisionPoint : null;
    }

    /**
     * Checks to see if the line defined by the passed-in start and end points intersects this box.
     * If so, then a new point is returned on the first point of intersection (outer bounds).  If the
     * line created by the two points does not intersect this box, null is returned.
     */
    public BoundingBoxHitResult getIntersection(Point3D start, Point3D end) {
        if (isOBB()) {
            return getOBBIntersection(start, end);
        }

        //First check minX.
        Point3D intersection = getXPlaneCollision(start, end, globalCenter.x - widthRadius);
        Axis hitSide = Axis.WEST;

        //Now get maxX.
        //If minX is null, or if maxX is not null, and is closer to the start point than minX, it's our new intersection.
        //Basically, we're getting the X- intersection here.
        Point3D secondIntersection = getXPlaneCollision(start, end, globalCenter.x + widthRadius);
        if (secondIntersection != null && (intersection == null || start.distanceTo(secondIntersection) < start.distanceTo(intersection))) {
            intersection = secondIntersection;
            hitSide = Axis.EAST;
        }

        //Now check minY.
        //If we don't have a valid intersection, or minY is closer than the current intersection, it's our new intersection.
        //This makes us chose between minY and X at this point.
        secondIntersection = getYPlaneCollision(start, end, globalCenter.y - heightRadius);
        if (secondIntersection != null && (intersection == null || start.distanceTo(secondIntersection) < start.distanceTo(intersection))) {
            intersection = secondIntersection;
            hitSide = Axis.DOWN;
        }

        //You should be able to see what we're doing here now, yes?
        //All we need to do is test maxY, minZ, and maxZ and we'll know where we hit.
        secondIntersection = getYPlaneCollision(start, end, globalCenter.y + heightRadius);
        if (secondIntersection != null && (intersection == null || start.distanceTo(secondIntersection) < start.distanceTo(intersection))) {
            intersection = secondIntersection;
            hitSide = Axis.UP;
        }
        secondIntersection = getZPlaneCollision(start, end, globalCenter.z - depthRadius);
        if (secondIntersection != null && (intersection == null || start.distanceTo(secondIntersection) < start.distanceTo(intersection))) {
            intersection = secondIntersection;
            hitSide = Axis.NORTH;
        }
        secondIntersection = getZPlaneCollision(start, end, globalCenter.z + depthRadius);
        if (secondIntersection != null && (intersection == null || start.distanceTo(secondIntersection) < start.distanceTo(intersection))) {
            intersection = secondIntersection;
            hitSide = Axis.SOUTH;
        }
        return intersection != null ? new BoundingBoxHitResult(this, intersection, hitSide) : null;
    }

    private BoundingBoxHitResult getOBBIntersection(Point3D start, Point3D end) {
        Point3D localStart = start.copy().subtract(globalCenter).reOrigin(orientation);
        Point3D localEnd = end.copy().subtract(globalCenter).reOrigin(orientation);
        Point3D localDelta = localEnd.copy().subtract(localStart);
        double[] startValues = { localStart.x, localStart.y, localStart.z };
        double[] deltaValues = { localDelta.x, localDelta.y, localDelta.z };
        double[] radii = { widthRadius, heightRadius, depthRadius };
        double minFactor = Double.NEGATIVE_INFINITY;
        double maxFactor = Double.POSITIVE_INFINITY;
        Axis minSide = Axis.NONE;
        Axis maxSide = Axis.NONE;
        Point3D minNormal = new Point3D();
        Point3D maxNormal = new Point3D();

        for (int axis = 0; axis < 3; ++axis) {
            if (Math.abs(deltaValues[axis]) < OBB_EPSILON) {
                if (startValues[axis] < -radii[axis] || startValues[axis] > radii[axis]) {
                    return null;
                }
            } else {
                double firstFactor = (-radii[axis] - startValues[axis]) / deltaValues[axis];
                double secondFactor = (radii[axis] - startValues[axis]) / deltaValues[axis];
                Axis firstSide = getOBBSide(axis, false);
                Axis secondSide = getOBBSide(axis, true);
                Point3D firstNormal = getOBBNormal(axis, false);
                Point3D secondNormal = getOBBNormal(axis, true);
                if (firstFactor > secondFactor) {
                    double priorFactor = firstFactor;
                    Axis priorSide = firstSide;
                    Point3D priorNormal = firstNormal;
                    firstFactor = secondFactor;
                    firstSide = secondSide;
                    firstNormal = secondNormal;
                    secondFactor = priorFactor;
                    secondSide = priorSide;
                    secondNormal = priorNormal;
                }
                if (firstFactor > minFactor) {
                    minFactor = firstFactor;
                    minSide = firstSide;
                    minNormal.set(firstNormal);
                }
                if (secondFactor < maxFactor) {
                    maxFactor = secondFactor;
                    maxSide = secondSide;
                    maxNormal.set(secondNormal);
                }
                if (minFactor > maxFactor) {
                    return null;
                }
            }
        }

        if (maxFactor < 0 || minFactor > 1) {
            return null;
        }
        double hitFactor = minFactor >= 0 ? minFactor : maxFactor;
        if (hitFactor < 0 || hitFactor > 1) {
            return null;
        }
        Point3D intersection = end.copy().subtract(start).scale(hitFactor).add(start);
        Axis hitSide = minFactor >= 0 ? minSide : maxSide;
        Point3D hitNormal = minFactor >= 0 ? minNormal : maxNormal;
        return new BoundingBoxHitResult(this, intersection, hitNormal, hitSide);
    }

    private Axis getOBBSide(int axis, boolean positive) {
        return Axis.getFromVector(getOBBNormal(axis, positive));
    }

    private Point3D getOBBNormal(int axis, boolean positive) {
        return new Point3D(axis == 0 ? (positive ? 1 : -1) : 0, axis == 1 ? (positive ? 1 : -1) : 0, axis == 2 ? (positive ? 1 : -1) : 0).rotate(orientation);
    }

    /**
     * Renders this bounding box as a wireframe model.
     * Automatically applies appropriate transforms to go from entity center to itself, or uses
     * the passed-in offset from global center if it is set.
     */
    public void renderWireframe(AEntityC_Renderable entity, TransformationMatrix transform, Point3D offset, ColorRGB color) {
        if (wireframeRenderable == null) {
            wireframeRenderable = new RenderableData(new RenderableVertices(false));
            if (definition != null) {
                if (definition.action != null) {
                    //Green for boxes that have actions.
                    wireframeRenderable.setColor(ColorRGB.GREEN);
                } else if (groupDef != null && groupDef.collisionTypes.contains(CollisionType.BULLET)) {
                    //Orange for bullet collisions.
                    wireframeRenderable.setColor(ColorRGB.ORANGE);
                } else if (groupDef != null && groupDef.collisionTypes.contains(CollisionType.BLOCK)) {
                    //Red for block collisions.
                    wireframeRenderable.setColor(ColorRGB.RED);
                } else {
                    //Black for general collisions.
                    wireframeRenderable.setColor(ColorRGB.BLACK);
                }
            } else {
                //Not a defined collision box.  Must be an interaction box.  Yellow.
                wireframeRenderable.setColor(ColorRGB.YELLOW);
            }
        }
        wireframeRenderable.transform.set(transform);
        helperPoint.set(globalCenter);
        if (offset != null) {
            helperPoint.add(offset);
        } else {
            helperPoint.subtract(entity.position);
        }
        wireframeRenderable.transform.applyTranslation(helperPoint);
        if (isOBB()) {
            wireframeRenderable.transform.applyRotation(orientation);
        }
        if (color != null) {
            //Override default color with set color.
            wireframeRenderable.setColor(color);
        }
        wireframeRenderable.setBoxBounds(this, true);
        wireframeRenderable.render();
    }

    /**
     * Renders this bounding box as a holographic model.  Does
     * not offset to its global position, as this might not play
     * nicely with the current matrix sate.
     */
    public void renderHolographic(TransformationMatrix transform, Point3D offset, ColorRGB color) {
        if (holographicRenderable == null) {
            holographicRenderable = new RenderableData(new RenderableVertices(true), "mts:textures/rendering/holobox.png");
            holographicRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        }
        holographicRenderable.transform.set(transform);
        if (offset != null) {
            holographicRenderable.transform.applyTranslation(offset);
        }
        holographicRenderable.setColor(color);
        holographicRenderable.setBoxBounds(this, false);
        holographicRenderable.render();
    }
}
