package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;

import mcinterface.WrapperBlock;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.vehicles.main.AEntityBase;

/**Basic bounding box.  This class is mutable and allows for quick setting of values
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
public class BoundingBox{
	public final Point3d localCenter;
	public final Point3d globalCenter;
	public final Point3d currentCollisionDepth;
	public final List<WrapperBlock> collidingBlocks = new ArrayList<WrapperBlock>();
	private final Point3d tempGlobalCenter;
	
	public double widthRadius;
	public double heightRadius;
	public double depthRadius;
	public final boolean collidesWithLiquids;
	public final boolean isInterior;
	
	public BoundingBox(Point3d center, double widthRadius, double heightRadius, double depthRadius){
		this(center, center, widthRadius, heightRadius, depthRadius, false, false);
	}
	
	public BoundingBox(Point3d localCenter, Point3d globalCenter, double widthRadius, double heightRadius, double depthRadius, boolean collidesWithLiquids, boolean isInterior){
		this.localCenter = localCenter;
		this.globalCenter = globalCenter;
		this.tempGlobalCenter = globalCenter.copy();
		this.currentCollisionDepth = new Point3d(0D, 0D, 0D);
		this.widthRadius = widthRadius;
		this.heightRadius = heightRadius;
		this.depthRadius = depthRadius;
		this.collidesWithLiquids = collidesWithLiquids;
		this.isInterior = isInterior;
	}
	
	/**
	 *  Populates the collidingBlocks list with all currently-colliding blocks.
	 *  Also populates the collisionDepth list with the collision depths of all collisions, not just blocks.
	 *  Note that the passed-in offset is only applied for this check,  and is reverted after this call.
	 *  If blocks collided with this box after this method, true is returned.
	 */
	public boolean updateCollidingBlocks(WrapperWorld world, Point3d offset){
		tempGlobalCenter.setTo(globalCenter);
		globalCenter.add(offset);
		world.updateBoundingBoxCollisions(this, offset);
		globalCenter.setTo(tempGlobalCenter);
		return !collidingBlocks.isEmpty();
	}
	
	/**
	 *  Sets the global center of this box to the position of the passed-in entity, rotated by the
	 *  entity's rotation and offset by the local center.  Mostly used for updating hitboxes that
	 *  rotate with the entity.  Rotation is done using the fine Point3d rotation to allow for
	 *  better interaction while standing on entities.
	 */
	public void updateToEntity(AEntityBase entity){
		globalCenter.setTo(localCenter).rotateFine(entity.angles).add(entity.position);
	}
	
	/**
	 *  Returns true if the passed-in point is inside this box.
	 */
	public boolean isPointInside(Point3d point){
		return 	globalCenter.x - widthRadius < point.x &&
				globalCenter.x + widthRadius > point.x &&
				globalCenter.y - heightRadius < point.y &&
				globalCenter.y + heightRadius > point.y &&
				globalCenter.z - depthRadius < point.z &&
				globalCenter.z + depthRadius > point.z;
	}
	
	/**
	 *  Returns true if the passed-in box intersects this box.
	 */
	public boolean intersects(BoundingBox box){
		return 	globalCenter.x - widthRadius < box.globalCenter.x + box.widthRadius &&
				globalCenter.x + widthRadius > box.globalCenter.x - box.widthRadius &&
				globalCenter.y - heightRadius < box.globalCenter.y + box.heightRadius &&
				globalCenter.y + heightRadius > box.globalCenter.y - box.heightRadius &&
				globalCenter.z - depthRadius < box.globalCenter.z + box.depthRadius &&
				globalCenter.z + depthRadius > box.globalCenter.z - box.depthRadius;
	}
	
	/**
	 *  Returns true if the passed-in point intersects this box in the YZ-plane.
	 */
	public boolean intersectsWithYZ(Point3d point){
        return point.y >= globalCenter.y - heightRadius && point.y <= globalCenter.y + heightRadius && point.z >= globalCenter.z - depthRadius && point.z <= globalCenter.z + depthRadius;
    }
	
	/**
	 *  Returns true if the passed-in point intersects this box in the XZ-plane.
	 */
	public boolean intersectsWithXZ(Point3d point){
        return point.x >= globalCenter.x - widthRadius && point.x <= globalCenter.x + widthRadius && point.z >= globalCenter.z - depthRadius && point.z <= globalCenter.z + depthRadius;
    }
	
	/**
	 *  Returns true if the passed-in point intersects this box in the XY-plane.
	 */
	public boolean intersectsWithXY(Point3d point){
        return point.x >= globalCenter.x - widthRadius && point.x <= globalCenter.x + widthRadius && point.y >= globalCenter.y - heightRadius && point.y <= globalCenter.y + heightRadius;
    }
	
	/**
	 *  Returns the point between the start and end points that collides with this box,
	 *  or null if such a point does not exist.
	 */
	public Point3d getXPPlaneCollision(Point3d start, Point3d end, double xPoint){
        Point3d collisionPoint = start.getIntermediateWithXValue(end, xPoint);
        return collisionPoint != null && this.intersectsWithYZ(collisionPoint) ? collisionPoint : null;
    }

	/**
	 *  Returns the point between the start and end points that collides with this box,
	 *  or null if such a point does not exist.
	 */
    public Point3d getYPlaneCollision(Point3d start, Point3d end, double yPoint){
    	Point3d collisionPoint = start.getIntermediateWithYValue(end, yPoint);
        return collisionPoint != null && this.intersectsWithXZ(collisionPoint) ? collisionPoint : null;
    }
    
    /**
	 *  Returns the point between the start and end points that collides with this box,
	 *  or null if such a point does not exist.
	 */
    public Point3d getZPlaneCollision(Point3d start, Point3d end, double zPoint){
    	Point3d collisionPoint = start.getIntermediateWithZValue(end, zPoint);
        return collisionPoint != null && this.intersectsWithXY(collisionPoint) ? collisionPoint : null;
    }
	
	/**
	 *  Checks to see if the line defined by the passed-in start and end points intersects this box.
	 *  If so, then a new point is returned on the first point of intersection (outer bounds).  If the
	 *  line created by the two points does not intersect this box, null is returned.
	 */
	public Point3d getIntersectionPoint(Point3d start, Point3d end){
		//First check minX.
		Point3d intersection = getXPPlaneCollision(start, end, globalCenter.x - widthRadius);
		
		//Now get maxX.
		Point3d secondIntersection = getXPPlaneCollision(start, end, globalCenter.x + widthRadius);
		
		//If minX is null, or if maxX is not null, and is closer to the start point than minX, it's our new intersection.
		if(secondIntersection != null && (intersection == null || start.distanceTo(secondIntersection) < start.distanceTo(intersection))){
			intersection = secondIntersection;
		}
		
		//Now check minY.
		secondIntersection = getYPlaneCollision(start, end, globalCenter.y - heightRadius);
		
		//If we don't have a valid intersection, or minY is closer than the current intersection, it's our new intersection.
		if(secondIntersection != null && (intersection == null || start.distanceTo(secondIntersection) < start.distanceTo(intersection))){
			intersection = secondIntersection;
		}
		
		//You should be able to see what we're doing here now, yes?
		//All we need to do is test maxY, minZ, and maxZ and we'll know where we hit.
		secondIntersection = getYPlaneCollision(start, end, globalCenter.y + heightRadius);
		if(secondIntersection != null && (intersection == null || start.distanceTo(secondIntersection) < start.distanceTo(intersection))){
			intersection = secondIntersection;
		}
		secondIntersection = getZPlaneCollision(start, end, globalCenter.z - depthRadius);
		if(secondIntersection != null && (intersection == null || start.distanceTo(secondIntersection) < start.distanceTo(intersection))){
			intersection = secondIntersection;
		}
		secondIntersection = getZPlaneCollision(start, end, globalCenter.z + depthRadius);
		if(secondIntersection != null && (intersection == null || start.distanceTo(secondIntersection) < start.distanceTo(intersection))){
			intersection = secondIntersection;
		}
		return intersection;
    }
}
