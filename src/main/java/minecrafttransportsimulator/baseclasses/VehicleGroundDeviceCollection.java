package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

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
	 */
	public void updateCollisions(){
		frontLeftGDB.updateCollisionStatuses();
		frontRightGDB.updateCollisionStatuses();
		rearLeftGDB.updateCollisionStatuses();
		rearRightGDB.updateCollisionStatuses();
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
		double side1Offset = 0;
		double side2Offset = 0;
		double oppositeSideOffset = 0;
		VehicleGroundDeviceBox testBox1 = null;
		VehicleGroundDeviceBox testBox2 = null;
		if(rearLeftGDB.isGrounded || rearRightGDB.isGrounded){
			if(!frontLeftGDB.isGrounded && !frontRightGDB.isGrounded){
				side1Offset = Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.z);
				side2Offset = Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.z);
				if(rearLeftGDB.isGrounded && !rearRightGDB.isGrounded){
					oppositeSideOffset = Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.z);
				}else if(!rearLeftGDB.isGrounded && rearRightGDB.isGrounded){
					oppositeSideOffset = Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.z);
				}else{
					oppositeSideOffset = Math.max(Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.z), Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.z));
				}
				testBox1 = frontLeftGDB;
				testBox2 = frontRightGDB;
			}
		}
		if(frontLeftGDB.isGrounded || frontRightGDB.isGrounded){
			if(!rearLeftGDB.isGrounded && !rearRightGDB.isGrounded){
				side1Offset = -Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.z);
				side2Offset = -Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.z);
				if(frontLeftGDB.isGrounded && !rearRightGDB.isGrounded){
					oppositeSideOffset = -Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.z);
				}else if(!frontLeftGDB.isGrounded && frontRightGDB.isGrounded){
					oppositeSideOffset = -Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.z);
				}else{
					oppositeSideOffset = -Math.max(Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.z), Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.z));
				}
				testBox1 = rearLeftGDB;
				testBox2 = rearRightGDB;
			}
		}
		side1Offset += oppositeSideOffset;
		side2Offset += oppositeSideOffset;
		
		//Only apply corrections if we have grounded offsets and the boxes are ready.
		if((side1Offset != 0 || side2Offset != 0) && testBox1.isReady() && testBox2.isReady()){
			//Now that we know we can rotate, get the angle required to put the boxes on the ground.
			//We first try to rotate the boxes by the max angle defined by the linear movement, if this doesn't ground
			//them, we return 0.  If it does ground them, then we inverse-calculate the exact angle required to ground them.
			double testRotation = Math.toDegrees(Math.asin(Math.min(MAX_LINEAR_MOVEMENT_PER_TICK/Math.max(side1Offset, side2Offset), 1)));
			
			//If we have negative motion.y, or our sign of our rotation matches the desired rotation of the vehicle apply it.
			//We need to take into account the ground boost here, as we might have some fake motion.y from prior GDB collisions.
			if(vehicle.motion.y - groundBoost*vehicle.SPEED_FACTOR <= 0 || testRotation*vehicle.rotation.x >= 0){
				//Add rotation and motion, and check for box collisions.
				double intialLinearMovement = Math.signum(oppositeSideOffset)*MAX_LINEAR_MOVEMENT_PER_TICK/vehicle.SPEED_FACTOR;
				vehicle.rotation.x += testRotation;
				vehicle.motion.y += intialLinearMovement;
				testBox1.updateCollisionStatuses();
				testBox2.updateCollisionStatuses();
				
				//Check if we collided after this movement.  If so, we need to calculate how far we need to angle to prevent collision. 
				double angularCorrection = 0;
				double linearCorrection = 0;
				if(testBox1.collisionDepth > 0){
					angularCorrection = Math.toDegrees(Math.asin(testBox1.collisionDepth/side1Offset));
					linearCorrection = Math.sin(Math.toRadians(testRotation - angularCorrection))*side1Offset/vehicle.SPEED_FACTOR;
				}
				if(testBox2.collisionDepth > 0){
					double angularCorrection2 = Math.toDegrees(Math.asin(testBox2.collisionDepth/side2Offset));
					if(angularCorrection2 > angularCorrection){
						angularCorrection = angularCorrection2;
						linearCorrection = Math.sin(Math.toRadians(testRotation - angularCorrection))*side2Offset/vehicle.SPEED_FACTOR;
					}
				}
				
				//Apply motions, rotations, re-calculate GDB states, and return applied motion.y for further processing.
				vehicle.rotation.x -= angularCorrection;
				vehicle.motion.y += linearCorrection;
				updateCollisions();
				return intialLinearMovement + linearCorrection;
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
		double side1Offset = 0;
		double side2Offset = 0;
		double oppositeSideOffset = 0;
		VehicleGroundDeviceBox testBox1 = null;
		VehicleGroundDeviceBox testBox2 = null;
		//front goes to right
		//back goes to left.
		//right goes to rear.
		//left goes to front.
		if(rearLeftGDB.isGrounded || rearRightGDB.isGrounded){
			if(!frontLeftGDB.isGrounded && !frontRightGDB.isGrounded){
				side1Offset = Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.z);
				side2Offset = Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.z);
				if(rearLeftGDB.isGrounded && !rearRightGDB.isGrounded){
					oppositeSideOffset = Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.z);
				}else if(!rearLeftGDB.isGrounded && rearRightGDB.isGrounded){
					oppositeSideOffset = Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.z);
				}else{
					oppositeSideOffset = Math.max(Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.z), Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.z));
				}
				testBox1 = frontLeftGDB;
				testBox2 = frontRightGDB;
			}
		}
		if(frontLeftGDB.isGrounded || frontRightGDB.isGrounded){
			if(!rearLeftGDB.isGrounded && !rearRightGDB.isGrounded){
				side1Offset = -Math.hypot(rearLeftGDB.contactPoint.y, rearLeftGDB.contactPoint.z);
				side2Offset = -Math.hypot(rearRightGDB.contactPoint.y, rearRightGDB.contactPoint.z);
				if(frontLeftGDB.isGrounded && !rearRightGDB.isGrounded){
					oppositeSideOffset = -Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.z);
				}else if(!frontLeftGDB.isGrounded && frontRightGDB.isGrounded){
					oppositeSideOffset = -Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.z);
				}else{
					oppositeSideOffset = -Math.max(Math.hypot(frontLeftGDB.contactPoint.y, frontLeftGDB.contactPoint.z), Math.hypot(frontRightGDB.contactPoint.y, frontRightGDB.contactPoint.z));
				}
				testBox1 = rearLeftGDB;
				testBox2 = rearRightGDB;
			}
		}
		side1Offset += oppositeSideOffset;
		side2Offset += oppositeSideOffset;
		
		//Only apply corrections if we have grounded offsets.
		if(side1Offset != 0 || side2Offset != 0){
			//Now that we know we can rotate, get the angle required to put the boxes on the ground.
			//We first try to rotate the boxes by the max angle defined by the linear movement, if this doesn't ground
			//them, we return 0.  If it does ground them, then we inverse-calculate the exact angle required to ground them.
			double testRotation = Math.toDegrees(Math.asin(Math.min(MAX_LINEAR_MOVEMENT_PER_TICK/Math.max(side1Offset, side2Offset), 1)));
			
			//If we have negative motion.y, or our sign of our rotation matches the desired rotation of the vehicle apply it.
			//We need to take into account the ground boost here, as we might have some fake motion.y from prior GDB collisions.
			if(vehicle.motion.y - groundBoost*vehicle.SPEED_FACTOR <= 0 || testRotation*vehicle.rotation.x >= 0){
				//Add rotation and motion, and check for box collisions.
				double intialLinearMovement = Math.signum(oppositeSideOffset)*MAX_LINEAR_MOVEMENT_PER_TICK/vehicle.SPEED_FACTOR;
				vehicle.rotation.x += testRotation;
				vehicle.motion.y += intialLinearMovement;
				testBox1.updateCollisionStatuses();
				testBox2.updateCollisionStatuses();
				
				//Check if we collided after this movement.  If so, we need to calculate how far we need to angle to prevent collision. 
				double angularCorrection = 0;
				double linearCorrection = 0;
				if(testBox1.collisionDepth > 0){
					angularCorrection = Math.toDegrees(Math.asin(testBox1.collisionDepth/side1Offset));
					linearCorrection = Math.sin(Math.toRadians(testRotation - angularCorrection))*side1Offset/vehicle.SPEED_FACTOR;
				}
				if(testBox2.collisionDepth > 0){
					double angularCorrection2 = Math.toDegrees(Math.asin(testBox2.collisionDepth/side2Offset));
					if(angularCorrection2 > angularCorrection){
						angularCorrection = angularCorrection2;
						linearCorrection = Math.sin(Math.toRadians(testRotation - angularCorrection))*side2Offset/vehicle.SPEED_FACTOR;
					}
				}
				
				//Apply motions, rotations, re-calculate GDB states, and return applied motion.y for further processing.
				vehicle.rotation.x -= angularCorrection;
				vehicle.motion.y += linearCorrection;
				updateCollisions();
				return intialLinearMovement + linearCorrection;
			}
		}
		return 0;
	}
}
