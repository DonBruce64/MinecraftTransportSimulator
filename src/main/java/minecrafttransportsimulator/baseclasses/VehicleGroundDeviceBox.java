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
	
	public boolean isCollided;
	public boolean isCollidedLiquid;
	public boolean isGrounded;
	public boolean isGroundedLiquid;
	public double collisionDepth;
	public final Point3d contactPoint = new Point3d(0D, 0D, 0D);
	
	//The following variables are only used for intermediary calculations.
	private BoundingBox currentBox;
	private final List<BoundingBox> liquidCollisionBoxes = new ArrayList<BoundingBox>();
	private final List<PartGroundDevice> groundDevices = new ArrayList<PartGroundDevice>();
	private final List<PartGroundDevice> liquidDevices = new ArrayList<PartGroundDevice>();
	
	public VehicleGroundDeviceBox(EntityVehicleF_Physics vehicle, boolean isFront, boolean isLeft){
		this.vehicle = vehicle;
		this.isFront = isFront;
		this.isLeft = isLeft;
		
		//Do an initial update once constructed.
		updateBoundingBoxes();
		updateCollisionStatuses();
	}
	
	/**
	 * Updates what bounding boxes make up this GDB.  These can change as parts are added and removed.
	 */
	public void updateBoundingBoxes(){
		//Get all liquid collision boxes.  Parts can add these via their collision boxes.
		liquidCollisionBoxes.clear();
		for(BoundingBox box : vehicle.collisionBoxes){
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
	 * Updates this boxes' collision properties to take into account its new position.
	 */
	public void updateCollisionStatuses(){
		//Initialize all values.
		isCollided = false;
		isGrounded = false;
		collisionDepth = 0;
		if(!groundDevices.isEmpty()){
			currentBox = getSolidPoint();
			currentBox.globalCenter.setTo(currentBox.localCenter).rotateCoarse(vehicle.angles.copy().add(vehicle.rotation)).add(vehicle.position);
			vehicle.world.updateBoundingBoxCollisions(currentBox, vehicle.motion.copy().multiply(vehicle.SPEED_FACTOR));
			isCollided = !currentBox.collidingBlocks.isEmpty();
			
			vehicle.world.updateBoundingBoxCollisions(currentBox, vehicle.motion.copy().multiply(vehicle.SPEED_FACTOR).add(PartGroundDevice.groundDetectionOffset));
			isGrounded = isCollided ? false : !currentBox.collidingBlocks.isEmpty();
			collisionDepth = currentBox.currentCollisionDepth.y;
			contactPoint.setTo(currentBox.localCenter).add(0D, -currentBox.heightRadius, 0D);
		}
		
		if(!liquidDevices.isEmpty() || !liquidCollisionBoxes.isEmpty()){
			currentBox = getLiquidPoint();
			currentBox.globalCenter.setTo(currentBox.localCenter).rotateCoarse(vehicle.angles.copy().add(vehicle.rotation)).add(vehicle.position);
			vehicle.world.updateBoundingBoxCollisions(currentBox, vehicle.motion.copy().multiply(vehicle.SPEED_FACTOR));
			isCollidedLiquid = currentBox.collidingBlocks.isEmpty();
			
			vehicle.world.updateBoundingBoxCollisions(currentBox, vehicle.motion.copy().multiply(vehicle.SPEED_FACTOR).add(PartGroundDevice.groundDetectionOffset));
			isGroundedLiquid = isCollidedLiquid ? false : !currentBox.collidingBlocks.isEmpty();
			double liquidCollisionDepth = currentBox.currentCollisionDepth.y;
			
			//If the liquid boxes are more collided, set collisions to those.
			//Otherwise, use the solid values.
			if(isCollidedLiquid && (liquidCollisionDepth > collisionDepth)){
				isCollided = isCollidedLiquid;
				isGrounded = isGroundedLiquid;
				collisionDepth = liquidCollisionDepth;
				contactPoint.setTo(currentBox.localCenter).add(0D, -currentBox.heightRadius, 0D);
			}
		}
	}
	
	/**
	 * Gets the solid collision point based on position of ground devices.
	 **/
	private BoundingBox getSolidPoint(){
		float heights = 0;
		float widths = 0;
		Point3d boxRelativePosition = new Point3d(0D, 0D, 0D);
		
		for(APart groundDevice : groundDevices){
			heights += groundDevice.getHeight();
			widths += groundDevice.getWidth();
			boxRelativePosition.add(groundDevice.totalOffset);
		}
		
		heights /= groundDevices.size();
		widths /= groundDevices.size();
		boxRelativePosition.multiply(1D/groundDevices.size());
		return new BoundingBox(boxRelativePosition, boxRelativePosition.copy(), widths, heights, widths, false, false);
	}
	
	/**Updates the liquid collision point based on position of liquid devices and collision boxes.**/
	private BoundingBox getLiquidPoint(){
		float heights = 0;
		float widths = 0;
		Point3d boxRelativePosition = new Point3d(0D, 0D, 0D);
		
		for(APart groundDevice : liquidDevices){
			heights += groundDevice.getHeight();
			widths += groundDevice.getWidth();
			boxRelativePosition.add(groundDevice.totalOffset);
		}
		
		for(BoundingBox box : liquidCollisionBoxes){
			heights += box.heightRadius*2D;
			widths += box.widthRadius*2D;
			boxRelativePosition.add(box.localCenter);
		}
		
		heights /= (liquidDevices.size() + liquidCollisionBoxes.size());
		widths /= (liquidDevices.size() + liquidCollisionBoxes.size());
		boxRelativePosition.multiply(1D/(liquidDevices.size() + liquidCollisionBoxes.size()));
		return new BoundingBox(boxRelativePosition, boxRelativePosition.copy(), widths, heights, widths, true, false);
	}
}
