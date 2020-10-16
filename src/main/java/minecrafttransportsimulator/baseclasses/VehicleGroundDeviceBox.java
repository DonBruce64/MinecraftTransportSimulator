package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;

/**This class is a wrapper for vehicle ground device collision points.  It's used to get a point
 * to reference for ground collisions, and contains helper methods for doing calculations of those
 * points.  Four of these can be used in a set to get four ground device points to use in
 * ground device operations on a vehicle.  Note that this class differentiates between floating
 * and non-floating objects, and includes collision boxes for the latter.  This ensures a
 * seamless transition from a floating to ground state in movement.
 * 
 * @author don_bruce
 */
public class VehicleGroundDeviceBox{
	private final EntityVehicleF_Physics vehicle;
	private final boolean isFront;
	private final boolean isLeft;
	private final BoundingBox solidBox = new BoundingBox(new Point3d(0D, 0D, 0D), new Point3d(0D, 0D, 0D), 0D, 0D, 0D, false, false, false, 0);
	private final BoundingBox liquidBox = new BoundingBox(new Point3d(0D, 0D, 0D), new Point3d(0D, 0D, 0D), 0D, 0D, 0D, true, false, false, 0);
	private final List<BoundingBox> liquidCollisionBoxes = new ArrayList<BoundingBox>();
	private final List<PartGroundDevice> groundDevices = new ArrayList<PartGroundDevice>();
	private final List<PartGroundDevice> liquidDevices = new ArrayList<PartGroundDevice>();
	
	public boolean isCollided;
	public boolean isCollidedLiquid;
	public boolean isGrounded;
	public boolean isGroundedLiquid;
	public double collisionDepth;
	public final Point3d contactPoint = new Point3d(0D, 0D, 0D);
	
	public VehicleGroundDeviceBox(EntityVehicleF_Physics vehicle, boolean isFront, boolean isLeft){
		this.vehicle = vehicle;
		this.isFront = isFront;
		this.isLeft = isLeft;
		
		//Do an initial update once constructed.
		updateMembers();
		updateBounds();
		updateCollisionStatuses();
	}
	
	/**
	 * Updates what objects make up this GDB.  These should change as parts are added and removed.
	 */
	public void updateMembers(){
		//Get all liquid collision boxes.  Parts can add these via their collision boxes.
		liquidCollisionBoxes.clear();
		for(BoundingBox box : vehicle.blockCollisionBoxes){
			if(box.collidesWithLiquids){
				if(isFront && box.localCenter.z > 0){
					if(isLeft && box.localCenter.x >= 0){
						liquidCollisionBoxes.add(box);
					}else if(!isLeft && box.localCenter.x <= 0){
						liquidCollisionBoxes.add(box);
					}
				}else if(!isFront && box.localCenter.z <= 0){
					if(isLeft && box.localCenter.x >= 0){
						liquidCollisionBoxes.add(box);
					}else if(!isLeft && box.localCenter.x <= 0){
						liquidCollisionBoxes.add(box);
					}
				}
			}
		}
		
		//Get all part-based collision boxes.  This includes solid and liquid ground devices.
		groundDevices.clear();
		liquidDevices.clear();
		for(APart part : vehicle.parts){
			if(part instanceof PartGroundDevice){
				//X-offsets of 0 are both left and right as they are center points.
				//This ensures we don't roll to try and align a center point.
				if(isFront && part.placementOffset.z > 0){
					if(isLeft && part.placementOffset.x >= 0){
						groundDevices.add((PartGroundDevice) part);
						if(part.definition.ground.canFloat){
							liquidDevices.add((PartGroundDevice) part);
						}
					}else if(!isLeft && part.placementOffset.x <= 0){
						groundDevices.add((PartGroundDevice) part);
						if(part.definition.ground.canFloat){
							liquidDevices.add((PartGroundDevice) part);
						}
					}
				}else if(!isFront && part.placementOffset.z <= 0){
					if(isLeft && part.placementOffset.x >= 0){
						groundDevices.add((PartGroundDevice) part);
						if(part.definition.ground.canFloat){
							liquidDevices.add((PartGroundDevice) part);
						}
					}else if(!isLeft && part.placementOffset.x <= 0){
						groundDevices.add((PartGroundDevice) part);
						if(part.definition.ground.canFloat){
							liquidDevices.add((PartGroundDevice) part);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Updates this boxes' bounds to match the included members.  This should only be done when we
	 * change members, or if a member has changed position.
	 */
	public void updateBounds(){
		//Update solid box local center and size.
		solidBox.localCenter.set(0D, 0D, 0D);
		solidBox.widthRadius = 0;
		solidBox.heightRadius = 0;
		for(APart groundDevice : groundDevices){
			solidBox.localCenter.add(groundDevice.totalOffset);
			solidBox.widthRadius += groundDevice.getWidth();
			solidBox.heightRadius += groundDevice.getHeight();
		}
		solidBox.widthRadius /= 2D*groundDevices.size();
		solidBox.heightRadius /= 2D*groundDevices.size();
		solidBox.depthRadius = solidBox.widthRadius;
		solidBox.localCenter.multiply(1D/groundDevices.size());
		
		//Update liquid box local center and size.
		liquidBox.localCenter.set(0D, 0D, 0D);
		liquidBox.widthRadius = 0;
		liquidBox.heightRadius = 0;
		for(APart groundDevice : liquidDevices){
			liquidBox.localCenter.add(groundDevice.totalOffset);
			liquidBox.widthRadius += groundDevice.getWidth();
			liquidBox.heightRadius += groundDevice.getHeight();
		}
		for(BoundingBox box : liquidCollisionBoxes){
			liquidBox.localCenter.add(box.localCenter);
			liquidBox.widthRadius += box.widthRadius*2D;
			liquidBox.heightRadius += box.heightRadius*2D;
		}
		liquidBox.widthRadius /= 2D*(liquidDevices.size() + liquidCollisionBoxes.size());
		liquidBox.heightRadius /= 2D*(liquidDevices.size() + liquidCollisionBoxes.size());
		liquidBox.depthRadius = solidBox.widthRadius;
		liquidBox.localCenter.multiply(1D/(liquidDevices.size() + liquidCollisionBoxes.size()));
	}
	
	/**
	 * Updates this boxes' collision properties to take into account its new position.
	 */
	public void updateCollisionStatuses(){
		//Initialize all values.
		isCollided = false;
		isGrounded = false;
		collisionDepth = 0;
		Point3d vehicleMotionOffset = vehicle.motion.copy().multiply(vehicle.SPEED_FACTOR);
		Point3d groundCollisionOffset = vehicleMotionOffset.copy().add(PartGroundDevice.groundDetectionOffset);
		if(!groundDevices.isEmpty()){
			solidBox.globalCenter.setTo(solidBox.localCenter).rotateCoarse(vehicle.angles.copy().add(vehicle.rotation)).add(vehicle.position).add(vehicleMotionOffset);
			vehicle.world.updateBoundingBoxCollisions(solidBox, vehicleMotionOffset, false);
			isCollided = !solidBox.collidingBlocks.isEmpty();
			collisionDepth = solidBox.currentCollisionDepth.y;
			
			solidBox.globalCenter.add(PartGroundDevice.groundDetectionOffset);
			vehicle.world.updateBoundingBoxCollisions(solidBox, groundCollisionOffset, false);
			solidBox.globalCenter.subtract(PartGroundDevice.groundDetectionOffset);
			isGrounded = isCollided ? true : !solidBox.collidingBlocks.isEmpty();
			contactPoint.setTo(solidBox.localCenter).add(0D, -solidBox.heightRadius, 0D);
		}
		
		if(!liquidDevices.isEmpty() || !liquidCollisionBoxes.isEmpty()){
			liquidBox.globalCenter.setTo(liquidBox.localCenter).rotateCoarse(vehicle.angles.copy().add(vehicle.rotation)).add(vehicle.position).add(vehicleMotionOffset);
			vehicle.world.updateBoundingBoxCollisions(liquidBox, vehicleMotionOffset, false);
			isCollidedLiquid = !liquidBox.collidingBlocks.isEmpty();
			double liquidCollisionDepth = liquidBox.currentCollisionDepth.y;
			
			liquidBox.globalCenter.add(PartGroundDevice.groundDetectionOffset);
			vehicle.world.updateBoundingBoxCollisions(liquidBox, groundCollisionOffset, false);
			liquidBox.globalCenter.subtract(PartGroundDevice.groundDetectionOffset);
			isGroundedLiquid = isCollidedLiquid ? true : !liquidBox.collidingBlocks.isEmpty();
			
			//If the liquid boxes are grounded and are more collided, use liquid values.
			//Otherwise, use the solid values.
			if(isGroundedLiquid && (liquidCollisionDepth >= collisionDepth)){
				isCollided = isCollidedLiquid;
				isGrounded = isGroundedLiquid;
				collisionDepth = liquidCollisionDepth;
				contactPoint.setTo(liquidBox.localCenter).add(0D, -liquidBox.heightRadius, 0D);
			}
		}
	}
	
	/**
	 * Returns true if this box has any boxes and is ready for collision operations.
	 */
	public boolean isReady(){
		return !groundDevices.isEmpty() || !liquidDevices.isEmpty();
	}
}
