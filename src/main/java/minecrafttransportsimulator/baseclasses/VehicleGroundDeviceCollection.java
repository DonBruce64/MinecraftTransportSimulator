package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;

/**This class is a collection for a set of four vehicle ground device points.  This allows for less
 * boilerplate code when we need to do operations on all four points in a vehicle.
 * 
 * @author don_bruce
 */
public class VehicleGroundDeviceCollection{
	private static final double MAX_LINEAR_MOVEMENT_PER_TICK = 0.2D;
	private final EntityVehicleF_Physics vehicle;
	private final VehicleGroundDeviceBox frontLeftGDB;
	private final VehicleGroundDeviceBox frontRightGDB;
	private final VehicleGroundDeviceBox rearLeftGDB;
	private final VehicleGroundDeviceBox rearRightGDB;
	public final List<PartGroundDevice> groundedGroundDevices = new ArrayList<PartGroundDevice>();
	
	public VehicleGroundDeviceCollection(EntityVehicleF_Physics vehicle){
		this.vehicle = vehicle;
		this.frontLeftGDB = new VehicleGroundDeviceBox(vehicle, true, true);
		this.frontRightGDB = new VehicleGroundDeviceBox(vehicle, true, false);
		this.rearLeftGDB = new VehicleGroundDeviceBox(vehicle, false, true);
		this.rearRightGDB = new VehicleGroundDeviceBox(vehicle, false, false);
	}
	
	/**
	 * Updates the members of all GDBs.
	 */
	public void updateMembers(){
		frontLeftGDB.updateMembers();
		frontRightGDB.updateMembers();
		rearLeftGDB.updateMembers();
		rearRightGDB.updateMembers();
	}
	
	/**
	 * Updates the bounding boxes for all GDBs.
	 */
	public void updateBounds(){
		frontLeftGDB.updateBounds();
		frontRightGDB.updateBounds();
		rearLeftGDB.updateBounds();
		rearRightGDB.updateBounds();
	}
	
	/**
	 * Updates all the boxes collision properties to take into account their new positions.
	 * Also re-calculates which ground devices are on the ground.
	 */
	public void updateCollisions(){
		groundedGroundDevices.clear();
		frontLeftGDB.updateCollisionStatuses(groundedGroundDevices);
		frontRightGDB.updateCollisionStatuses(groundedGroundDevices);
		rearLeftGDB.updateCollisionStatuses(groundedGroundDevices);
		rearRightGDB.updateCollisionStatuses(groundedGroundDevices);
	}
	
	/**
	 * Spawns block particles under all wheels if they are part of a collective that's on the ground.
	 * Used for fancy slipping animations.  Only call this on the CLIENT!
	 */
	public void spawnSlippingParticles(){
		if(frontLeftGDB.isGrounded){
			MasterLoader.renderInterface.spawnBlockBreakParticles(new Point3i(frontLeftGDB.contactPoint.copy().rotateCoarse(vehicle.angles).add(vehicle.position)).add(0, -1, 0), false);
		}
		if(frontRightGDB.isGrounded){
			MasterLoader.renderInterface.spawnBlockBreakParticles(new Point3i(frontRightGDB.contactPoint.copy().rotateCoarse(vehicle.angles).add(vehicle.position)).add(0, -1, 0), false);
		}
		if(rearLeftGDB.isGrounded){
			MasterLoader.renderInterface.spawnBlockBreakParticles(new Point3i(rearLeftGDB.contactPoint.copy().rotateCoarse(vehicle.angles).add(vehicle.position)).add(0, -1, 0), false);
		}
		if(rearRightGDB.isGrounded){
			MasterLoader.renderInterface.spawnBlockBreakParticles(new Point3i(rearRightGDB.contactPoint.copy().rotateCoarse(vehicle.angles).add(vehicle.position)).add(0, -1, 0), false);
		}
	}
	
	/**
	 * Gets the max collision depth for all boxes.
	 */
	public double getMaxCollisionDepth(){
		return Math.max(Math.max(Math.max(frontLeftGDB.collisionDepth, frontRightGDB.collisionDepth), rearLeftGDB.collisionDepth), rearRightGDB.collisionDepth);
	}
	
	/**
	 * Gets the number of boxes in liquid.
	 */
	public int getBoxesInLiquid(){
		int boxes = 0;
		if(frontLeftGDB.isCollidedLiquid || frontLeftGDB.isGroundedLiquid){
			++boxes;
		}
		if(frontRightGDB.isCollidedLiquid || frontRightGDB.isGroundedLiquid){
			++boxes;
		}
		if(rearLeftGDB.isCollidedLiquid || rearLeftGDB.isGroundedLiquid){
			++boxes;
		}
		if(rearRightGDB.isCollidedLiquid || rearRightGDB.isGroundedLiquid){
			++boxes;
		}
		return boxes;
	}
	
	/**
	 * Returns true if the boxes are ready for ground calculations.  In essence, this checks for a front and back box,
	 * plus a left or right box if one of those boxes aren't centered.
	 */
	public boolean isReady(){
		boolean haveFrontPoint = false;
		boolean haveRearPoint = false;
		boolean haveCenterPoint = false;
		if(frontLeftGDB.isReady()){
			haveFrontPoint = true;
			haveCenterPoint = frontLeftGDB.contactPoint.x == 0;
		}
		if(frontRightGDB.isReady()){
			if(haveFrontPoint){
				haveCenterPoint = true;
			}else{
				haveFrontPoint = true;
			}
			if(!haveCenterPoint){
				haveCenterPoint = frontRightGDB.contactPoint.x == 0;
			}
		}
		if(haveFrontPoint){
			if(rearLeftGDB.isReady()){
				haveRearPoint = true;
				haveCenterPoint = rearLeftGDB.contactPoint.x == 0;
			}
			if(rearRightGDB.isReady()){
				if(haveRearPoint){
					haveCenterPoint = true;
				}else{
					haveRearPoint = true;
				}
				if(!haveCenterPoint){
					haveCenterPoint = rearRightGDB.contactPoint.x == 0;
				}
			}
		}
		return haveFrontPoint && haveRearPoint && haveCenterPoint;
	}
	
	/**
	 * Returns true if the passed-in ground device is on the ground.  This queries the appropriate GDB
	 * for the ground device and checks if it is on the ground.  This way we use a single point of
	 * state for grounded-ness instead of recalculating it every check.
	 */
	public boolean isDeviceOnGround(PartGroundDevice ground){
		if(ground.placementOffset.z > 0){
			if(ground.placementOffset.x > 0){
				return frontLeftGDB.isGrounded;
			}else{
				return frontRightGDB.isGrounded;
			}
		}else{
			if(ground.placementOffset.x > 0){
				return rearLeftGDB.isGrounded;
			}else{
				return rearRightGDB.isGrounded;
			}
		}
	}
	
	/**
	 * Returns true if the passed-in ground device can provide motive force.  This checks the vehicle's drivetrain to see
	 * if it could power the ground device.  Note that just because the ground device can provide power, doesn't mean it is.
	 * Wheels in the air don't do much good.  For this reason, ensure checks for force use {@link #isDeviceOnGround(PartGroundDevice)}
	 */
	public boolean canDeviceProvideForce(PartGroundDevice ground){
		return (ground.placementOffset.z > 0 && vehicle.definition.motorized.isFrontWheelDrive) || (ground.placementOffset.z <= 0 && vehicle.definition.motorized.isRearWheelDrive);
	}
	
	/**
	 * Corrects pitch for the GDBs, returning the amount of motion.y that the system had to apply to perform the correction.
	 * This amount is determined by checking which GDBs are on the ground, and which are free. 
	 * Angles are applied internally and then motion.y is added to level everything out.
	 * If no GDBs are on the ground, 0 is returned.  If alternate GDBs are on the ground,
	 * such as the front-left and rear-right, 0 is also returned, as there is no way to apply
	 * rotation in this situation to make any others GDBs be on the ground without removing the ones currently on the ground.
	 * 
	 */
	public double performPitchCorrection(double groundBoost){
		//Get the offset and test boxes for this correction.
		boolean invertedCheck = false;
		double side1Delta = 0;
		double side2Delta = 0;
		double groundedSideOffset = 0;
		VehicleGroundDeviceBox testBox1 = null;
		VehicleGroundDeviceBox testBox2 = null;
		if(vehicle.towedByVehicle == null){
			if(rearLeftGDB.isGrounded || rearRightGDB.isGrounded){
				if(!frontLeftGDB.isGrounded && !frontRightGDB.isGrounded){
					side1Delta = Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.z);
					side2Delta = Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.z);
					if(rearLeftGDB.isGrounded && !rearRightGDB.isGrounded){
						groundedSideOffset = -Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.z);
					}else if(!rearLeftGDB.isGrounded && rearRightGDB.isGrounded){
						groundedSideOffset = -Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.z);
					}else{
						groundedSideOffset = -Math.max(Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.z), Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.z));
					}
					testBox1 = frontLeftGDB;
					testBox2 = frontRightGDB;
				}
			}
			if(frontLeftGDB.isGrounded || frontRightGDB.isGrounded){
				if(!rearLeftGDB.isGrounded && !rearRightGDB.isGrounded){
					side1Delta = -Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.z);
					side2Delta = -Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.z);
					if(frontLeftGDB.isGrounded && !rearRightGDB.isGrounded){
						groundedSideOffset = Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.z);
					}else if(!frontLeftGDB.isGrounded && frontRightGDB.isGrounded){
						groundedSideOffset = Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.z);
					}else{
						groundedSideOffset = Math.max(Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.z), Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.z));
					}
					testBox1 = rearLeftGDB;
					testBox2 = rearRightGDB;
				}
			}
		}else{
			if(!rearLeftGDB.isGrounded && !rearRightGDB.isGrounded){
				side1Delta = -Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.z);
				side2Delta = -Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.z);
				groundedSideOffset = Math.hypot(vehicle.definition.motorized.hookupPos.y, vehicle.definition.motorized.hookupPos.z);
				testBox1 = rearLeftGDB;
				testBox2 = rearRightGDB;
			}else if(rearLeftGDB.isCollided || rearRightGDB.isCollided){
				//Need to do inverted logic here. In this case, we want to rotate if we DO have a collision.
				//Get the max collision depth and rotate by that amount.
				//This logic is similar to the global function, but has some simplifications.
				//First populate variables.
				side1Delta = Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.z);
				side2Delta = Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.z);
				groundedSideOffset = -Math.hypot(vehicle.definition.motorized.hookupPos.y, vehicle.definition.motorized.hookupPos.z);
				side1Delta -= groundedSideOffset;
				side2Delta -= groundedSideOffset;
				
				//Now get the rotation required to get the collision boxes un-collided.
				double angularCorrection = 0;
				if(rearLeftGDB.collisionDepth > 0){
					angularCorrection = Math.toDegrees(Math.asin(rearLeftGDB.collisionDepth/side1Delta));
				}
				if(rearRightGDB.collisionDepth > 0){
					double angularCorrection2 = Math.toDegrees(Math.asin(rearRightGDB.collisionDepth/side2Delta));
					if(angularCorrection > 0 ? angularCorrection2 > angularCorrection : angularCorrection2 < angularCorrection){
						angularCorrection = angularCorrection2;
					}
				}
				
				//Get the linear correction required for this angular correction.
				//If the linear correction is larger than the max linear, factor it.
				double linearCorrection = Math.sin(Math.toRadians(angularCorrection))*groundedSideOffset;
				if(Math.abs(linearCorrection) > MAX_LINEAR_MOVEMENT_PER_TICK){
					linearCorrection = Math.signum(linearCorrection)*MAX_LINEAR_MOVEMENT_PER_TICK;
					angularCorrection = Math.toDegrees(Math.asin(linearCorrection/groundedSideOffset));
				}
				
				//If the angular correction isn't NaN, apply and return it.  Otherwise, return 0.
				if(Double.isNaN(angularCorrection)){
					return 0;
				}else{
					//Apply motions, rotations, re-calculate GDB states, and return applied motion.y for further processing.
					vehicle.rotation.x += angularCorrection;
					vehicle.motion.y += linearCorrection;
					updateCollisions();
					return linearCorrection;
				}
			}
		}
		side1Delta -= groundedSideOffset;
		side2Delta -= groundedSideOffset;
		
		//Only apply corrections if we have grounded offsets and the boxes are ready.
		if((side1Delta != 0 || side2Delta != 0) && testBox1.isReady() && testBox2.isReady()){
			//Now that we know we can rotate, get the angle required to put the boxes on the ground.
			//We first try to rotate the boxes by the max angle defined by the linear movement, if this doesn't ground
			//them, we return 0.  If it does ground them, then we inverse-calculate the exact angle required to ground them.
			double testRotation = Math.toDegrees(Math.asin(Math.min(MAX_LINEAR_MOVEMENT_PER_TICK/Math.max(side1Delta, side2Delta), 1)));
			
			//If we have negative motion.y, or our sign of our rotation matches the desired rotation of the vehicle apply it.
			//We need to take into account the ground boost here, as we might have some fake motion.y from prior GDB collisions.
			if((vehicle.motion.y - groundBoost*vehicle.SPEED_FACTOR <= 0 || testRotation*vehicle.rotation.x >= 0) && Math.abs(vehicle.angles.x + testRotation) < 85){
				//Add rotation and motion, and check for box collisions.
				double intialLinearMovement = Math.sin(Math.toRadians(testRotation))*groundedSideOffset;
				vehicle.rotation.x += testRotation;
				vehicle.motion.y += intialLinearMovement;
				testBox1.updateCollisionStatuses(null);
				testBox2.updateCollisionStatuses(null);
				
				//Check if we collided after this movement.  If so, we need to calculate how far we need to angle to prevent collision.
				double angularCorrection = 0;
				double linearCorrection = 0;
				if(testBox1.collisionDepth > 0){
					angularCorrection = Math.toDegrees(Math.asin(testBox1.collisionDepth/side1Delta));
				}
				if(testBox2.collisionDepth > 0){
					double angularCorrection2 = Math.toDegrees(Math.asin(testBox2.collisionDepth/side2Delta));
					if(angularCorrection > 0 ? angularCorrection2 > angularCorrection : angularCorrection2 < angularCorrection){
						angularCorrection = angularCorrection2;
					}
				}
				//If the angular correction is greater than our initial angle, don't change movement.
				//This can happen if we rotate via an angle into a block.
				if(Math.abs(angularCorrection) > Math.abs(testRotation) || Double.isNaN(angularCorrection)){
					vehicle.rotation.x -= testRotation;
					vehicle.motion.y -= intialLinearMovement;
					return 0;
				}else{
					//Apply motions, rotations, re-calculate GDB states, and return applied motion.y for further processing.
					linearCorrection = -intialLinearMovement*(angularCorrection/testRotation);
					vehicle.rotation.x -= angularCorrection;
					vehicle.motion.y += linearCorrection;
					updateCollisions();
					return intialLinearMovement + linearCorrection;
				}
			}
		}
		return 0;
	}
	
	/**
	 * Corrects roll for the GDBs, returning the amount of motion.y that the system had to apply to perform the correction.
	 * This amount is determined by checking which GDBs are on the ground, and which are free. 
	 * Angles are applied internally and then motion.y is added to level everything out.
	 * If no GDBs are on the ground, 0 is returned.  If alternate GDBs are on the ground,
	 * such as the front-left and rear-right, 0 is also returned, as there is no way to apply
	 * rotation in this situation to make any others GDBs be on the ground without removing the ones currently on the ground.
	 * 
	 */
	public double performRollCorrection(double groundBoost){
		//Get the offset and test boxes for this correction.
		double side1Delta = 0;
		double side2Delta = 0;
		double groundedSideOffset = 0;
		VehicleGroundDeviceBox testBox1 = null;
		VehicleGroundDeviceBox testBox2 = null;
		if(rearRightGDB.isGrounded || frontRightGDB.isGrounded){
			if(!rearLeftGDB.isGrounded && !frontLeftGDB.isGrounded){
				side1Delta = Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.x);
				side2Delta = Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.x);
				if(rearRightGDB.isGrounded && !frontRightGDB.isGrounded){
					groundedSideOffset = -Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.x);
				}else if(!rearRightGDB.isGrounded && frontRightGDB.isGrounded){
					groundedSideOffset = -Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.x);
				}else{
					groundedSideOffset = -Math.max(Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.x), Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.x));
				}
				testBox1 = frontLeftGDB;
				testBox2 = frontRightGDB;
			}
		}
		if(rearLeftGDB.isGrounded || frontLeftGDB.isGrounded){
			if(!rearRightGDB.isGrounded && !frontRightGDB.isGrounded){
				side1Delta = -Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.x);
				side2Delta = -Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.x);
				if(rearLeftGDB.isGrounded && !frontRightGDB.isGrounded){
					groundedSideOffset = Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.x);
				}else if(!rearLeftGDB.isGrounded && frontLeftGDB.isGrounded){
					groundedSideOffset = Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.x);
				}else{
					groundedSideOffset = Math.max(Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.x), Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.x));
				}
				testBox1 = rearLeftGDB;
				testBox2 = rearRightGDB;
			}
		}
		side1Delta -= groundedSideOffset;
		side2Delta -= groundedSideOffset;
		
		//Only apply corrections if we have grounded offsets and the boxes are ready.
		if((side1Delta != 0 || side2Delta != 0) && testBox1.isReady() && testBox2.isReady()){
			//Now that we know we can rotate, get the angle required to put the boxes on the ground.
			//We first try to rotate the boxes by the max angle defined by the linear movement, if this doesn't ground
			//them, we return 0.  If it does ground them, then we inverse-calculate the exact angle required to ground them.
			double testRotation = -Math.toDegrees(Math.asin(Math.min(MAX_LINEAR_MOVEMENT_PER_TICK/Math.max(side1Delta, side2Delta), 1)));
			
			//If we have negative motion.y, or our sign of our rotation matches the desired rotation of the vehicle apply it.
			//We need to take into account the ground boost here, as we might have some fake motion.y from prior GDB collisions.
			if(vehicle.motion.y - groundBoost*vehicle.SPEED_FACTOR <= 0 || testRotation*vehicle.rotation.z >= 0){
				//Add rotation and motion, and check for box collisions.
				double intialLinearMovement = -Math.sin(Math.toRadians(testRotation))*groundedSideOffset;
				vehicle.rotation.z += testRotation;
				vehicle.motion.y += intialLinearMovement;
				testBox1.updateCollisionStatuses(null);
				testBox2.updateCollisionStatuses(null);
				
				//Check if we collided after this movement.  If so, we need to calculate how far we need to angle to prevent collision. 
				double angularCorrection = 0;
				double linearCorrection = 0;
				if(testBox1.collisionDepth > 0){
					angularCorrection = Math.toDegrees(Math.asin(testBox1.collisionDepth/side1Delta));
				}
				if(testBox2.collisionDepth > 0){
					double angularCorrection2 = Math.toDegrees(Math.asin(testBox2.collisionDepth/side2Delta));
					if(angularCorrection > 0 ? angularCorrection2 > angularCorrection : angularCorrection2 < angularCorrection){
						angularCorrection = angularCorrection2;
					}
				}
				//If the angular correction is greater than our initial angle, don't change movement.
				//This can happen if we rotate via an angle into a block.
				if(Math.abs(angularCorrection) > Math.abs(testRotation) || Double.isNaN(angularCorrection)){
					vehicle.rotation.z -= testRotation;
					vehicle.motion.y -= intialLinearMovement;
					return 0;
				}else{
					//Apply motions, rotations, re-calculate GDB states, and return applied motion.y for further processing.
					linearCorrection = -intialLinearMovement*(-angularCorrection/testRotation);
					vehicle.rotation.z += angularCorrection;
					vehicle.motion.y += linearCorrection;
					updateCollisions();
					return intialLinearMovement + linearCorrection;
				}
			}
		}
		return 0;
	}
}
