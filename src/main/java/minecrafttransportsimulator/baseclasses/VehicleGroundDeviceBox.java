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
	private final BoundingBox solidBox = new BoundingBox(new Point3d(), new Point3d(), 0D, 0D, 0D, false, false, false, 0);
	private final BoundingBox liquidBox = new BoundingBox(new Point3d(), new Point3d(), 0D, 0D, 0D, true, false, false, 0);
	private final List<BoundingBox> liquidCollisionBoxes = new ArrayList<BoundingBox>();
	private final List<PartGroundDevice> groundDevices = new ArrayList<PartGroundDevice>();
	private final List<PartGroundDevice> liquidDevices = new ArrayList<PartGroundDevice>();
	
	public boolean isCollided;
	public boolean isCollidedLiquid;
	public boolean isGrounded;
	public boolean isGroundedLiquid;
	public boolean isAbleToDoGroundOperations;
	public boolean isAbleToDoGroundOperationsLiquid;
	public boolean isLiquidCollidedWithGround;
	public double collisionDepth;
	public final Point3d contactPoint = new Point3d();
	
	public VehicleGroundDeviceBox(EntityVehicleF_Physics vehicle, boolean isFront, boolean isLeft){
		this.vehicle = vehicle;
		this.isFront = isFront;
		this.isLeft = isLeft;
		
		//Do an initial update once constructed.
		updateMembers();
		updateBounds();
		updateCollisionStatuses(null);
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
				if(!part.placementDefinition.isSpare){
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
	}
	
	/**
	 * Updates this boxes' bounds to match the included members.  This should only be done when we
	 * change members, or if a member has changed position.
	 */
	public void updateBounds(){
		//Update solid box local center and size.
		//We use the lowest-contacting ground device for size.
		//Position is average for XZ, and min for Y.
		solidBox.localCenter.set(0D, Double.MAX_VALUE, 0D);
		solidBox.widthRadius = 0;
		solidBox.heightRadius = 0;
		for(APart groundDevice : groundDevices){
			solidBox.localCenter.x += groundDevice.localOffset.x;
			solidBox.localCenter.z += groundDevice.localOffset.z;
			if(groundDevice.localOffset.y - groundDevice.getHeight()/2D < solidBox.localCenter.y - solidBox.heightRadius){
				solidBox.localCenter.y = groundDevice.localOffset.y;
				solidBox.heightRadius = groundDevice.getHeight()/2D;
				solidBox.widthRadius = groundDevice.getWidth()/2D;
			}
		}
		solidBox.depthRadius = solidBox.widthRadius;
		solidBox.localCenter.x *= 1D/groundDevices.size();
		solidBox.localCenter.z *= 1D/groundDevices.size();
		
		//Update liquid box local center and size.
		liquidBox.localCenter.set(0D, Double.MAX_VALUE, 0D);
		liquidBox.widthRadius = 0;
		liquidBox.heightRadius = 0;
		for(APart groundDevice : liquidDevices){
			liquidBox.localCenter.x += groundDevice.localOffset.x;
			liquidBox.localCenter.z += groundDevice.localOffset.z;
			if(groundDevice.localOffset.y - groundDevice.getHeight()/2D < liquidBox.localCenter.y - liquidBox.heightRadius){
				liquidBox.localCenter.y = groundDevice.localOffset.y;
				liquidBox.heightRadius = groundDevice.getHeight()/2D;
				liquidBox.widthRadius = groundDevice.getWidth()/2D;
			}
		}
		for(BoundingBox box : liquidCollisionBoxes){
			liquidBox.localCenter.x += box.localCenter.x;
			liquidBox.localCenter.z += box.localCenter.z;
			if(box.localCenter.y - box.heightRadius < liquidBox.localCenter.y - liquidBox.heightRadius){
				liquidBox.localCenter.y = box.localCenter.y;
				liquidBox.heightRadius = box.heightRadius;
				liquidBox.widthRadius = box.widthRadius;
			}
		}
		liquidBox.depthRadius = liquidBox.widthRadius;
		liquidBox.localCenter.x *= 1D/(liquidDevices.size() + liquidCollisionBoxes.size());
		liquidBox.localCenter.z *= 1D/(liquidDevices.size() + liquidCollisionBoxes.size());
	}
	
	/**
	 * Updates this boxes' collision properties to take into account its new position.
	 * If the passed-in list is non-null, all grounded ground devices will be added to it.
	 */
	public void updateCollisionStatuses(List<PartGroundDevice> groundedGroundDevices){
		//Initialize all values.
		isCollided = false;
		isGrounded = false;
		isAbleToDoGroundOperations = false;
		collisionDepth = 0;
		Point3d vehicleMotionOffset = vehicle.motion.copy().multiply(EntityVehicleF_Physics.SPEED_FACTOR);
		Point3d groundCollisionOffset = vehicleMotionOffset.copy().add(PartGroundDevice.groundDetectionOffset);
		if(!groundDevices.isEmpty()){
			solidBox.globalCenter.setTo(solidBox.localCenter).rotateFine(vehicle.angles.copy().add(vehicle.rotation)).add(vehicle.position).add(vehicleMotionOffset);
			vehicle.world.updateBoundingBoxCollisions(solidBox, vehicleMotionOffset, false);
			isCollided = !solidBox.collidingBlockPositions.isEmpty();
			collisionDepth = solidBox.currentCollisionDepth.y;
			PartGroundDevice.groundOperationOffset.set(0 , -0.5, 0);
			if(isCollided){
				isGrounded = true;
			}else{
				solidBox.globalCenter.add(PartGroundDevice.groundDetectionOffset);
				vehicle.world.updateBoundingBoxCollisions(solidBox, groundCollisionOffset, false);
				solidBox.globalCenter.subtract(PartGroundDevice.groundDetectionOffset);
				isGrounded = !solidBox.collidingBlockPositions.isEmpty();
				contactPoint.setTo(solidBox.localCenter).add(0D, -solidBox.heightRadius, 0D);
			}
			
			if(isCollided || isGrounded){
				isAbleToDoGroundOperations = true;
			}else{
				groundCollisionOffset = vehicleMotionOffset.copy().add(PartGroundDevice.groundOperationOffset);
				solidBox.globalCenter.add(PartGroundDevice.groundOperationOffset);
				vehicle.world.updateBoundingBoxCollisions(solidBox, groundCollisionOffset, false);
				solidBox.globalCenter.subtract(PartGroundDevice.groundOperationOffset);
				isAbleToDoGroundOperations = !solidBox.collidingBlockPositions.isEmpty();
			}
		}
		
		if(!liquidDevices.isEmpty() || !liquidCollisionBoxes.isEmpty()){
			liquidBox.globalCenter.setTo(liquidBox.localCenter).rotateFine(vehicle.angles.copy().add(vehicle.rotation)).add(vehicle.position).add(vehicleMotionOffset);
			vehicle.world.updateBoundingBoxCollisions(liquidBox, vehicleMotionOffset, false);
			isCollidedLiquid = !liquidBox.collidingBlockPositions.isEmpty();
			double liquidCollisionDepth = liquidBox.currentCollisionDepth.y;
			
			if(isCollidedLiquid){
				isGroundedLiquid = true;
			}else{
				liquidBox.globalCenter.add(PartGroundDevice.groundDetectionOffset);
				vehicle.world.updateBoundingBoxCollisions(liquidBox, groundCollisionOffset, false);
				liquidBox.globalCenter.subtract(PartGroundDevice.groundDetectionOffset);
				isGroundedLiquid = !liquidBox.collidingBlockPositions.isEmpty();
			}
			
			if(isCollidedLiquid || isGroundedLiquid){
				isAbleToDoGroundOperationsLiquid = true;
			}else{
				groundCollisionOffset = vehicleMotionOffset.copy().add(PartGroundDevice.groundOperationOffset);
				liquidBox.globalCenter.add(PartGroundDevice.groundOperationOffset);
				vehicle.world.updateBoundingBoxCollisions(liquidBox, groundCollisionOffset, false);
				liquidBox.globalCenter.subtract(PartGroundDevice.groundOperationOffset);
				isAbleToDoGroundOperationsLiquid = !liquidBox.collidingBlockPositions.isEmpty();
			}
			
			isLiquidCollidedWithGround = false;
			for(Point3d blockPosition : liquidBox.collidingBlockPositions){
				if(!vehicle.world.isBlockLiquid(blockPosition)){
					isLiquidCollidedWithGround = true;
					break;
				}
			}
			
			//If the liquid boxes are grounded and are more collided, use liquid values.
			//Otherwise, use the solid values.
			if(isGroundedLiquid && (liquidCollisionDepth >= collisionDepth)){
				isCollided = isCollidedLiquid;
				isGrounded = isGroundedLiquid;
				isAbleToDoGroundOperations = isAbleToDoGroundOperationsLiquid;
				collisionDepth = liquidCollisionDepth;
				contactPoint.setTo(liquidBox.localCenter).add(0D, -liquidBox.heightRadius, 0D);
			}
		}
		
		//Add ground devices to the list.
		if(groundedGroundDevices != null && isAbleToDoGroundOperations){
			groundedGroundDevices.addAll(groundDevices);
		}
	}
	
	/**
	 * Returns true if this box has any boxes and is ready for collision operations.
	 */
	public boolean isReady(){
		return !groundDevices.isEmpty() || !liquidCollisionBoxes.isEmpty() || !liquidDevices.isEmpty();
	}
	
	/**
	 * Returns the bounding box that currently represents this box.  This can change depending on what
	 * ground devices we have and if we are colliding with liquids or solids.
	 */
	public BoundingBox getBoundingBox(){
		return isAbleToDoGroundOperationsLiquid ? liquidBox : solidBox;
	}
}
