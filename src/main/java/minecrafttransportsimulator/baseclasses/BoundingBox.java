package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.components.RenderableObject;
import net.minecraft.util.math.AxisAlignedBB;

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
	private static final double HITBOX_CLAMP = 0.015625;
	public final Point3d localCenter;
	public final Point3d globalCenter;
	public final Point3d currentCollisionDepth;
	public final List<Point3d> collidingBlockPositions = new ArrayList<Point3d>();
	public final RenderableObject renderable;
	private final Point3d tempGlobalCenter;
	
	public double widthRadius;
	public double heightRadius;
	public double depthRadius;
	public final boolean collidesWithLiquids;
	public final JSONCollisionBox definition;
	
	/**Simple constructor.  Used for blocks, bounds checks, or other things that don't need local/global positional differences.**/
	public BoundingBox(Point3d center, double widthRadius, double heightRadius, double depthRadius){
		this(center, center, widthRadius, heightRadius, depthRadius, false, null, null);
	}
	
	/**Complex constructor.  Used for things that have local and global positions.  These can also collide with liquid blocks.**/
	public BoundingBox(Point3d localCenter, Point3d globalCenter, double widthRadius, double heightRadius, double depthRadius, boolean collidesWithLiquids){
		this(localCenter, globalCenter, widthRadius, heightRadius, depthRadius, collidesWithLiquids, null, null);
	}
	
	/**JSON constructor.  Used for boxes that are created from JSON and need extended properties.**/
	public BoundingBox(JSONCollisionBox definition, JSONCollisionGroup groupDef){
		this(definition.pos, definition.pos.copy(), definition.width/2D, definition.height/2D, definition.width/2D, definition.collidesWithLiquids, definition, groupDef);
	}
	
	/**Master constructor.  Used for main creation.**/
	private BoundingBox(Point3d localCenter, Point3d globalCenter, double widthRadius, double heightRadius, double depthRadius, boolean collidesWithLiquids, JSONCollisionBox definition, JSONCollisionGroup groupDef){
		this.localCenter = localCenter;
		this.globalCenter = globalCenter;
		this.tempGlobalCenter = globalCenter.copy();
		this.currentCollisionDepth = new Point3d();
		this.widthRadius = widthRadius;
		this.heightRadius = heightRadius;
		this.depthRadius = depthRadius;
		this.collidesWithLiquids = collidesWithLiquids;
		this.definition = definition;
		
		final ColorRGB boxColor;
		if(definition != null){
			if(definition.variableName != null){
				//Green for boxes that activate variables..
				boxColor = ColorRGB.GREEN;
			}else if(groupDef != null && !groupDef.isInterior){
				//Red for block collisions.
				boxColor = ColorRGB.RED;
			}else{
				//Black for general collisions.
				boxColor = ColorRGB.BLACK;
			}
		}else{
			//Not a defined collision box.  Must be an interaction box.  Yellow.
			boxColor = ColorRGB.YELLOW;
		}
		this.renderable = new RenderableObject(this, new ColorRGB(boxColor.rgbInt), false);
	}
	
	@Override
	public String toString(){
		return "LocalCenter:" + localCenter.toString() + " GlobalCenter:" + globalCenter.toString() + " Width:" + widthRadius + " Height:" + heightRadius + " Depth:" + depthRadius; 
	}
	
	/**
	 *  Populates the collidingBlocks list with all currently-colliding blocks.
	 *  Note that the passed-in offset is only applied for this check,  and is reverted after this call.
	 *  If blocks collided with this box after this method, true is returned.
	 */
	public boolean updateCollidingBlocks(WrapperWorld world, Point3d offset){
		return updateCollisions(world, offset, false);
	}
	
	/**
	 *  Like {@link #updateCollidingBlocks(WrapperWorld, Point3d)}, but takes movement into account
	 *  when setting collision depth.
	 */
	public boolean updateMovingCollisions(WrapperWorld world, Point3d offset){
		return updateCollisions(world, offset, true);
	}
	
	private boolean updateCollisions(WrapperWorld world, Point3d offset, boolean ignoreIfGreater){
		tempGlobalCenter.setTo(globalCenter);
		globalCenter.add(offset);
		world.updateBoundingBoxCollisions(this, offset, ignoreIfGreater);
		globalCenter.setTo(tempGlobalCenter);
		return !collidingBlockPositions.isEmpty();
	}
	
	/**
	 *  Sets the global center of this box to the position of the passed-in entity, rotated by the
	 *  entity's rotation and offset by the local center.  Mostly used for updating hitboxes that
	 *  rotate with the entity.  Rotation is done using the fine Point3d rotation to allow for
	 *  better interaction while standing on entities.  Optional extra offset is present should
	 *  a supplemental translation need to be performed before aligning to the entity.
	 */
	public void updateToEntity(AEntityD_Definable<?> entity, Point3d optionalOffset){
		globalCenter.setTo(localCenter);
		if(optionalOffset != null){
			globalCenter.add(optionalOffset);
		}
		entity.orientation.net.rotatePoint(globalCenter).add(entity.position);
		if(definition != null){
			//Need to round box to prevent floating-point errors for player and entity collision.
			globalCenter.x = ((int) (globalCenter.x/HITBOX_CLAMP))*HITBOX_CLAMP;
			globalCenter.y = ((int) (globalCenter.y/HITBOX_CLAMP))*HITBOX_CLAMP;
			globalCenter.z = ((int) (globalCenter.z/HITBOX_CLAMP))*HITBOX_CLAMP;
		}
		//If we are on the client, and are rendering bounding boxes, update the position.
		if(entity.world.isClient() && InterfaceRender.shouldRenderBoundingBoxes()){
			renderable.setWireframeBoundingBox(this);
		}
	}
	
	/**
	 *  Returns the edge points for this box.
	 *  Order is the 4 -x points, then the 4 +x.
	 *  Y is -y for two, +y for two.
	 *  Z alternates each point.
	 */
	public float[][] getEdgePoints(){
		float[][] points = new float[8][];
		for(int i=0; i<2; ++i){
			for(int j=0; j<2; ++j){
				for(int k=0; k<2; ++k){
					points[i*4 + j*2 + k] = new float[]{
						(float) (i == 0 ? -widthRadius : +widthRadius),
						(float) (j == 0 ? -heightRadius : +heightRadius),
						(float) (k == 0 ? -depthRadius : +depthRadius)
					};
				}
			}	
		}
		return points;
	}
	
	/**
	 *  Returns true if the passed-in point is inside this box.
	 *  Note that this returns true for points on the border, to allow use to use in
	 *  in conjunction with hit-scanning code to find out which box got hit-scanned.
	 */
	public boolean isPointInside(Point3d point){
		return 	globalCenter.x - widthRadius <= point.x &&
				globalCenter.x + widthRadius >= point.x &&
				globalCenter.y - heightRadius <= point.y &&
				globalCenter.y + heightRadius >= point.y &&
				globalCenter.z - depthRadius <= point.z &&
				globalCenter.z + depthRadius >= point.z;
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
	public Point3d getXPlaneCollision(Point3d start, Point3d end, double xPoint){
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
		Point3d intersection = getXPlaneCollision(start, end, globalCenter.x - widthRadius);
		
		//Now get maxX.
		Point3d secondIntersection = getXPlaneCollision(start, end, globalCenter.x + widthRadius);
		
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
	
	/**
	 *  Helper method to convert the BoundingBox to an AxisAlignedBB.
	 */
	public AxisAlignedBB convert(){
		return new AxisAlignedBB(
			globalCenter.x - widthRadius,
			globalCenter.y - heightRadius,
			globalCenter.z - depthRadius,
			globalCenter.x + widthRadius,
			globalCenter.y + heightRadius,
			globalCenter.z + depthRadius
		);
	}
	
	/**
	 *  Helper method to convert the BoundingBox to an AxisAlignedBB.
	 *  This method allows for an offset to the conversion, to prevent
	 *  creating two AABBs (the conversion and the offset box).
	 */
	public AxisAlignedBB convertWithOffset(double x, double y, double z){
		return new AxisAlignedBB(
			x + globalCenter.x - widthRadius,
			y + globalCenter.y - heightRadius,
			z + globalCenter.z - depthRadius,
			x + globalCenter.x + widthRadius,
			y + globalCenter.y + heightRadius,
			z + globalCenter.z + depthRadius
		);
	}
}
