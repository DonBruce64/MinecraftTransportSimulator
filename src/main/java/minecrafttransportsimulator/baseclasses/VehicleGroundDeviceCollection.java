package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;

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
	public final Set<PartGroundDevice> groundedGroundDevices = new HashSet<PartGroundDevice>();
	public final Set<PartGroundDevice> drivenWheels = new HashSet<PartGroundDevice>();
	
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
		drivenWheels.clear();
		for(PartGroundDevice ground : frontLeftGDB.getGroundDevices()){
			if(!ground.placementDefinition.isSpare && ground.isActive && (ground.definition.ground.isWheel || ground.definition.ground.isTread) && (vehicle.definition.motorized.isFrontWheelDrive)){
				drivenWheels.add(ground);
			}
		}
		for(PartGroundDevice ground : frontRightGDB.getGroundDevices()){
			if(!ground.placementDefinition.isSpare && ground.isActive && (ground.definition.ground.isWheel || ground.definition.ground.isTread) && (vehicle.definition.motorized.isFrontWheelDrive)){
				drivenWheels.add(ground);
			}
		}
		for(PartGroundDevice ground : rearLeftGDB.getGroundDevices()){
			if(!ground.placementDefinition.isSpare && ground.isActive && (ground.definition.ground.isWheel || ground.definition.ground.isTread) && (vehicle.definition.motorized.isRearWheelDrive)){
				drivenWheels.add(ground);
			}
		}
		for(PartGroundDevice ground : rearRightGDB.getGroundDevices()){
			if(!ground.placementDefinition.isSpare && ground.isActive && (ground.definition.ground.isWheel || ground.definition.ground.isTread) && (vehicle.definition.motorized.isRearWheelDrive)){
				drivenWheels.add(ground);
			}
		}
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
	 * Gets the max collision depth for all boxes.
	 */
	public double getMaxCollisionDepth(){
		return Math.max(Math.max(Math.max(frontLeftGDB.collisionDepth, frontRightGDB.collisionDepth), rearLeftGDB.collisionDepth), rearRightGDB.collisionDepth);
	}
	
	/**
	 * Gets the number of liquid boxes that are collided with the ground.
	 */
	public int getNumberCollidedLiquidBoxes(){
		int count = 0;
		if(frontLeftGDB.isGroundedLiquid && frontLeftGDB.isLiquidCollidedWithGround){
			++count;
		}
		if(frontRightGDB.isGroundedLiquid && frontRightGDB.isLiquidCollidedWithGround){
			++count;
		}
		if(rearLeftGDB.isGroundedLiquid && rearLeftGDB.isLiquidCollidedWithGround){
			++count;
		}
		if(rearRightGDB.isGroundedLiquid && rearRightGDB.isLiquidCollidedWithGround){
			++count;
		}
		return count;
	}
	
	/**
	 * Gets the number of boxes in liquid.  Only valid if the box is allowed
	 * to collide with liquids.
	 */
	public int getNumberBoxesInLiquid(){
		int count = 0;
		if(frontLeftGDB.isCollidedLiquid || frontLeftGDB.isGroundedLiquid){
			++count;
		}
		if(frontRightGDB.isCollidedLiquid || frontRightGDB.isGroundedLiquid){
			++count;
		}
		if(rearLeftGDB.isCollidedLiquid || rearLeftGDB.isGroundedLiquid){
			++count;
		}
		if(rearRightGDB.isCollidedLiquid || rearRightGDB.isGroundedLiquid){
			++count;
		}
		return count;
	}
	
	/**
	 * Gets the bounding boxes that make up this ground collective.
	 * This will be the four ground points, or less if we don't have them.
	 */
	public List<BoundingBox> getGroundBounds(){
		List<BoundingBox> groundBoxes = new ArrayList<BoundingBox>();
		groundBoxes.add(frontLeftGDB.getBoundingBox());
		groundBoxes.add(frontRightGDB.getBoundingBox());
		groundBoxes.add(rearLeftGDB.getBoundingBox());
		groundBoxes.add(rearRightGDB.getBoundingBox());
		return groundBoxes;
	}
	
	/**
	 * Return the following point for this collective for either the front or rear vehicle points.
	 * This is based on the average of the contact points for the ground devices.
	 * If there are no ground devices for the contact point, null is returned.
	 * Note that this point is in the vehicle's local coordinates.
	 */
	public Point3d getContactPoint(boolean front){
		if(front){
			if(frontLeftGDB.contactPoint.isZero()){
				if(frontRightGDB.contactPoint.isZero()){
					return null;
				}else{
					return frontRightGDB.contactPoint.copy().add(PartGroundDevice.groundDetectionOffset);
				}
			}else{
				if(frontRightGDB.contactPoint.isZero()){
					return frontRightGDB.contactPoint.copy().add(PartGroundDevice.groundDetectionOffset);
				}else{
					return frontRightGDB.contactPoint.copy().subtract(frontLeftGDB.contactPoint).multiply(0.5).add(frontLeftGDB.contactPoint).add(PartGroundDevice.groundDetectionOffset);
				}
			}
		}else{
			if(rearLeftGDB.contactPoint.isZero()){
				if(rearRightGDB.contactPoint.isZero()){
					return null;
				}else{
					return rearRightGDB.contactPoint.copy().add(PartGroundDevice.groundDetectionOffset);
				}
			}else{
				if(rearRightGDB.contactPoint.isZero()){
					return rearRightGDB.contactPoint.copy().add(PartGroundDevice.groundDetectionOffset);
				}else{
					return rearRightGDB.contactPoint.copy().subtract(rearLeftGDB.contactPoint).multiply(0.5).add(rearLeftGDB.contactPoint).add(PartGroundDevice.groundDetectionOffset);
				}
			}
		}
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
	 * Returns true if any devices are on the ground.
	 */
	public boolean isAnythingOnGround(){
		return frontLeftGDB.isGrounded || frontRightGDB.isGrounded || rearLeftGDB.isGrounded || rearRightGDB.isGrounded;
	}
	
	/**
	 * Returns true if the passed-in device is actually on the ground.
	 * This is different than the {@link #groundedGroundDevices}, as
	 * this is the actual on-ground state rather than the state where phyiscs
	 * calculations can be performed.
	 */
	public boolean isActuallyOnGround(PartGroundDevice groundDevice){
		if(frontLeftGDB.isPartofBox(groundDevice)) return frontLeftGDB.isGrounded;
		if(frontRightGDB.isPartofBox(groundDevice)) return frontRightGDB.isGrounded;
		if(rearLeftGDB.isPartofBox(groundDevice)) return rearLeftGDB.isGrounded;
		if(rearRightGDB.isPartofBox(groundDevice)) return rearRightGDB.isGrounded;
		return false;
	}
	
	/**
	 * Returns true if the boxes in this collective can do roll operations.
	 * More formally, it checks that they aren't all aligned on the Z-axis.
	 */
	public boolean canDoRollChecks(){
		double xAxisPoint = 0;
		if(frontLeftGDB != null){
			xAxisPoint = frontLeftGDB.contactPoint.x;
		}
		if(frontRightGDB != null && xAxisPoint == 0){
			xAxisPoint = frontRightGDB.contactPoint.x;
		}
		if(rearLeftGDB != null && xAxisPoint == 0){
			xAxisPoint = rearLeftGDB.contactPoint.x;
		}
		if(rearRightGDB != null && xAxisPoint == 0){
			xAxisPoint = rearRightGDB.contactPoint.x;
		}
		return xAxisPoint != 0;
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
		double side1Delta = 0;
		double side2Delta = 0;
		double groundedSideOffset = 0;
		VehicleGroundDeviceBox testBox1 = null;
		VehicleGroundDeviceBox testBox2 = null;
		if(vehicle.towedByConnection == null){
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
			Point3d hookupPoint = vehicle.towedByConnection.hookupConnection.pos.copy();
			if(vehicle.towedByConnection.hookupEntity instanceof APart){
				APart hookupPart = (APart) vehicle.towedByConnection.hookupEntity;
				hookupPoint = hookupPoint.rotateFine(hookupPart.localAngles).add(hookupPart.localOffset);
			}
			if(hookupPoint.z > 0){
				if(!rearLeftGDB.isGrounded && !rearRightGDB.isGrounded){
					side1Delta = -Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.z);
					side2Delta = -Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.z);
					groundedSideOffset = Math.hypot(hookupPoint.y, hookupPoint.z);
					testBox1 = rearLeftGDB;
					testBox2 = rearRightGDB;
				}else if(rearLeftGDB.isCollided || rearRightGDB.isCollided){
					side1Delta = Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.z);
					side2Delta = Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.z);
					groundedSideOffset = -Math.hypot(hookupPoint.y, hookupPoint.z);
					side1Delta -= groundedSideOffset;
					side2Delta -= groundedSideOffset;
					testBox1 = rearLeftGDB;
					testBox2 = rearRightGDB;
					return adjustTrailerAngles(testBox1, testBox2, side1Delta, side2Delta, groundedSideOffset, groundBoost);
				}
			}else{
				if(!frontLeftGDB.isGrounded && !frontRightGDB.isGrounded){
					side1Delta = Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.z);
					side2Delta = Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.z);
					groundedSideOffset = -Math.hypot(hookupPoint.y, hookupPoint.z);
					testBox1 = frontLeftGDB;
					testBox2 = frontRightGDB;
				}else if(frontLeftGDB.isCollided || frontRightGDB.isCollided){
					side1Delta = -Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.z);
					side2Delta = -Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.z);
					groundedSideOffset = Math.hypot(hookupPoint.y, hookupPoint.z);
					side1Delta -= groundedSideOffset;
					side2Delta -= groundedSideOffset;
					testBox1 = frontLeftGDB;
					testBox2 = frontRightGDB;
					return adjustTrailerAngles(testBox1, testBox2, side1Delta, side2Delta, groundedSideOffset, groundBoost);
				}
			}
		}
	
		side1Delta -= groundedSideOffset;
		side2Delta -= groundedSideOffset;
		return adjustAngles(testBox1, testBox2, side1Delta, side2Delta, groundedSideOffset, groundBoost, true);
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
		double side1Delta = 0;
		double side2Delta = 0;
		double groundedSideOffset = 0;
		VehicleGroundDeviceBox testBox1 = null;
		VehicleGroundDeviceBox testBox2 = null;
		if(rearLeftGDB.isGrounded || frontLeftGDB.isGrounded){
			if((!rearRightGDB.isGrounded || rearRightGDB.contactPoint.x == 0) && (!frontRightGDB.isGrounded || frontRightGDB.contactPoint.x == 0)){
				side1Delta = Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.x);
				side2Delta = Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.x);
				if(rearLeftGDB.isGrounded && !frontLeftGDB.isGrounded){
					groundedSideOffset = -Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.x);
				}else if(!rearLeftGDB.isGrounded && frontLeftGDB.isGrounded){
					groundedSideOffset = -Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.x);
				}else{
					groundedSideOffset = -Math.max(Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.x), Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.x));
				}
				testBox1 = rearRightGDB;
				testBox2 = frontRightGDB;
			}
		}
		if(rearRightGDB.isGrounded || frontRightGDB.isGrounded){
			if((!rearLeftGDB.isGrounded || rearLeftGDB.contactPoint.x == 0) && (!frontLeftGDB.isGrounded || frontLeftGDB.contactPoint.x == 0)){
				side1Delta = -Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.x);
				side2Delta = -Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.x);
				if(rearRightGDB.isGrounded && !frontRightGDB.isGrounded){
					groundedSideOffset = Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.x);
				}else if(!rearRightGDB.isGrounded && frontRightGDB.isGrounded){
					groundedSideOffset = Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.x);
				}else{
					groundedSideOffset = Math.max(Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.x), Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.x));
				}
				testBox1 = rearLeftGDB;
				testBox2 = frontLeftGDB;
			}
		}
		
		side1Delta -= groundedSideOffset;
		side2Delta -= groundedSideOffset;
		return adjustAngles(testBox1, testBox2, side1Delta, side2Delta, groundedSideOffset, groundBoost, false);
	}
	
	/**
	 * Helper function to adjust angles in a common manner for pitch and roll calculations.
	 * Returns the y-motion added to adjust the angles.
	 */
	private double adjustAngles(VehicleGroundDeviceBox testBox1, VehicleGroundDeviceBox testBox2, double side1Delta, double side2Delta, double groundedSideOffset, double groundBoost, boolean pitch){
		//Only apply corrections if we have grounded offsets and the boxes are ready.
		if((side1Delta != 0 || side2Delta != 0) && testBox1.isReady() && testBox2.isReady()){
			//Now that we know we can rotate, get the angle required to put the boxes on the ground.
			//We first try to rotate the boxes by the max angle defined by the linear movement, if this doesn't ground
			//them, we return 0.  If it does ground them, then we inverse-calculate the exact angle required to ground them.
			double testSin = MAX_LINEAR_MOVEMENT_PER_TICK/Math.max(side1Delta, side2Delta);
			if(testSin > 1){
				testSin = 1;
			}else if(testSin < -1){
				testSin = -1;
			}
			double testRotation = Math.toDegrees(Math.asin(testSin));
			
			//If we are moving towards the ground, apply corrections.
			//We need to take into account the ground boost here, as we might have some fake motion.y from prior GDB collisions.
			//We also don't want to apply correction if it would cause us to pitch in excess of 85 degrees.
			//This prevents wall-climbing and other odd physics.
			if(vehicle.motion.y - groundBoost <= 0.01 && (pitch ? Math.abs(vehicle.angles.x + testRotation) < 85 : true)){
				//Add rotation and motion, and check for box collisions.
				//Check if correction is in opposite to requested rotation.
				///If so, don't apply it and bail.
				double intialLinearMovement = Math.sin(Math.toRadians(testRotation))*groundedSideOffset;
				if(pitch){
					if(vehicle.rotation.x*testRotation < 0 && vehicle.velocity > 0.5){
						return 0;
					}else{
						vehicle.rotation.x += testRotation;
					}
				}else{
					vehicle.rotation.z += testRotation;
				}
				vehicle.motion.y += intialLinearMovement;
				testBox1.updateCollisionStatuses(null);
				testBox2.updateCollisionStatuses(null);
				
				//Check if we collided after this movement.  If so, we need to calculate how far we need to angle to prevent collision.
				//If we are tilting and a box along the axis centerline collided, ignore it, as these shouldn't be taken into account.
				double angularCorrection = 0;
				double linearCorrection = 0;
				if(testBox1.collisionDepth > 0 && (pitch ? testBox1.contactPoint.z != 0 : testBox1.contactPoint.x != 0)){
					angularCorrection = Math.toDegrees(Math.asin(testBox1.collisionDepth/side1Delta));
				}
				if(testBox2.collisionDepth > 0 && (pitch ? testBox2.contactPoint.z != 0 : testBox2.contactPoint.x != 0)){
					double angularCorrection2 = Math.toDegrees(Math.asin(testBox2.collisionDepth/side2Delta));
					if(angularCorrection == 0 || (angularCorrection > 0 ? angularCorrection2 > angularCorrection : angularCorrection2 < angularCorrection)){
						angularCorrection = angularCorrection2;
					}
				}
				//If the angular correction is greater than our initial angle, don't change movement.
				//This can happen if we rotate via an angle into a block.
				if(angularCorrection != 0 && (Math.abs(angularCorrection) > Math.abs(testRotation) || Double.isNaN(angularCorrection))){
					if(pitch){
						vehicle.rotation.x -= testRotation;
					}else{
						vehicle.rotation.z -= testRotation;
					}
					vehicle.motion.y -= intialLinearMovement;
					return 0;
				}else{
					//Apply motions, rotations, re-calculate GDB states, and return applied motion.y for further processing.
					linearCorrection = intialLinearMovement*(angularCorrection/testRotation);
					if(pitch){
						vehicle.rotation.x -= angularCorrection;
					}else{
						vehicle.rotation.z -= angularCorrection;
					}
					vehicle.motion.y -= linearCorrection;
					updateCollisions();
					if(testBox1.isCollided || testBox2.isCollided){
						//We have bad math here.  Likely due to tilting ground devices.
						//Add only 5% of the rotation and just call it good.
						double finalAngularMovement = (testRotation - angularCorrection)*0.05;
						double finalLinearMovement = (intialLinearMovement - linearCorrection)*0.05;
						if(pitch){
							vehicle.rotation.x -= (testRotation - angularCorrection - finalAngularMovement);
						}else{
							vehicle.rotation.z -= (testRotation - angularCorrection - finalAngularMovement);
						}
						vehicle.motion.y -= (intialLinearMovement - linearCorrection - finalLinearMovement);
						updateCollisions();
						return finalLinearMovement;
					}else{
						return intialLinearMovement - linearCorrection;
					}
				}
			}
		}
		return 0;
	}
	
	/**
	 * Helper function to adjust angles for pitch for trailers.
	 * Need to do inverted logic here. In this case, we want to rotate if we DO have a collision.
	 * Get the max collision depth and rotate by that amount.
	 * This logic is similar to the global function, but has some simplifications.
	 */
	private double adjustTrailerAngles(VehicleGroundDeviceBox testBox1, VehicleGroundDeviceBox testBox2, double side1Delta, double side2Delta, double groundedSideOffset, double groundBoost){
		//Get the rotation required to get the collision boxes un-collided.
		double angularCorrection = 0;
		if(testBox1.collisionDepth > 0){
			angularCorrection = Math.toDegrees(Math.asin(testBox1.collisionDepth/side1Delta));
		}
		if(testBox2.collisionDepth > 0){
			double angularCorrection2 = Math.toDegrees(Math.asin(testBox2.collisionDepth/side2Delta));
			if(angularCorrection == 0 || (angularCorrection > 0 ? angularCorrection2 > angularCorrection : angularCorrection2 < angularCorrection)){
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
