package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.List;

import mcinterface.BuilderEntity;
import mcinterface.InterfaceNetwork;
import mcinterface.WrapperBlock;
import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.baseclasses.VehicleGroundDeviceBox;
import minecrafttransportsimulator.packets.instances.PacketVehicleServerMovement;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;

/**At the final basic vehicle level we add in the functionality for state-based movement.
 * Here is where the functions for moving permissions, such as collision detection
 * routines and ground device effects come in.  We also add functionality to keep
 * servers and clients from de-syncing.  At this point we now have a basic vehicle
 *  that can be manipulated for movement in the world.  
 * 
 * @author don_bruce
 */
abstract class EntityVehicleD_Moving extends EntityVehicleC_Colliding{

	//External state control.
	public boolean brakeOn;
	public boolean parkingBrakeOn;
	public boolean locked;
	public String ownerUUID = "";
	public String displayText = "";
	
	//Internal states.
	private final Point3d serverDeltaM;
	private final Point3d serverDeltaA;
	private final Point3d clientDeltaM;
	private final Point3d clientDeltaA;
	private final Point3d clientMApplied = new Point3d(0D, 0D, 0D);
	private final Point3d clientAApplied = new Point3d(0D, 0D, 0D);
	private final Point3d tempBoxPosition = new Point3d(0D, 0D, 0D);
	private final Point3d tempBoxAngles = new Point3d(0D, 0D, 0D);
	
	private int brakingTime = 10;
	private int throttleTime = 10;
	private double bodyAcceleration;
	private double forceOfInertia;
	private double bodyBrakeAngle;
	private double bodyAcclAngle;
	
	//Constants.
	private static final double MAX_ROTATION_RAD_PER_TICK = 0.0174533D*2D;
	
    //Classes used for ground device collisions.
  	protected VehicleGroundDeviceBox frontLeftGroundDeviceBox;
  	protected VehicleGroundDeviceBox frontRightGroundDeviceBox;
  	protected VehicleGroundDeviceBox rearLeftGroundDeviceBox;
  	protected VehicleGroundDeviceBox rearRightGroundDeviceBox;
    
	/**List of ground devices on the ground.  Populated after each movement to be used in turning/braking calculations.*/
	protected final List<PartGroundDevice> groundedGroundDevices = new ArrayList<PartGroundDevice>();
	
	
	public EntityVehicleD_Moving(BuilderEntity builder, WrapperWorld world, WrapperNBT data){
		super(builder, world, data);
		this.locked = data.getBoolean("locked");
		this.parkingBrakeOn = data.getBoolean("parkingBrakeOn");
		this.brakeOn = data.getBoolean("brakeOn");
		this.ownerUUID = data.getString("ownerUUID");
		this.displayText = data.getString("displayText");
		if(displayText.isEmpty()){
			displayText = definition.rendering.defaultDisplayText;
		}
		
		this.serverDeltaM = data.getPoint3d("serverDeltaM");
		this.serverDeltaA = data.getPoint3d("serverDeltaA");
		this.clientDeltaM = serverDeltaM.copy();
		this.clientDeltaA = serverDeltaA.copy();
	}
	
	@Override
	public void update(){
		super.update();
		//Populate the ground device lists for use in the methods here.
		//We need to get which ground devices are in which quadrant,
		//as well as which ground devices are on the ground.
		//This needs to be done before movement calculations so we can do checks during them.
		groundedGroundDevices.clear();
		for(APart part : parts){
			if(part instanceof PartGroundDevice){
				if(((PartGroundDevice) part).isOnGround()){
					groundedGroundDevices.add((PartGroundDevice) part);
				}
			}
		}
		
		//Create boxes if we haven't yet.
		//Update the BoundingBoxes that make them up if we have.
		if(frontLeftGroundDeviceBox == null){
			frontLeftGroundDeviceBox = new VehicleGroundDeviceBox((EntityVehicleF_Physics) this, true, true);
			frontRightGroundDeviceBox = new VehicleGroundDeviceBox((EntityVehicleF_Physics) this, true, false);
			rearLeftGroundDeviceBox = new VehicleGroundDeviceBox((EntityVehicleF_Physics) this, false, true);
			rearRightGroundDeviceBox = new VehicleGroundDeviceBox((EntityVehicleF_Physics) this, false, false);
		}else{
			frontLeftGroundDeviceBox.updateBoundingBoxes();
			frontRightGroundDeviceBox.updateBoundingBoxes();
			rearLeftGroundDeviceBox.updateBoundingBoxes();
			rearRightGroundDeviceBox.updateBoundingBoxes();
		}
		
		//Now do update calculations and logic.
		getForcesAndMotions();
		performGroundOperations();
		moveVehicle();
		if(!world.isClient()){
			dampenControlSurfaces();
		}
		
		//Update parking brake angle.
		//FIXME remove this with the duration/delay code.
		prevParkingBrakeAngle = parkingBrakeAngle;
		if(parkingBrakeOn && !locked && velocity < 0.25){
			if(parkingBrakeAngle < 30){
				prevParkingBrakeAngle = parkingBrakeAngle;
				++parkingBrakeAngle;
			}
		}else{
			if(parkingBrakeAngle > 0){
				prevParkingBrakeAngle = parkingBrakeAngle;
				--parkingBrakeAngle;
			}
		}
	}
	
	
//Calculating Force of Inertia which will give mow much force is needed that is to be applied
//to the suspensions when accelerating and braking to derive the angle.
	
	//Accelerating animation which calculates the amount of time the throttle is pressed to derive the acceleration
	//to then get the value of force exerted by inertia.
	public double acclInertia() {
		
		forceOfInertia = currentMass*(bodyAcceleration);

		
		if (velocity > prevVelocity) {
			
			throttleTime++;
			
		}else {
			
			throttleTime = 0;
		}
		
        if(throttleTime > 10 && !parkingBrakeOn && groundVelocity >= 3){
        	
        	bodyAcceleration = (groundVelocity/throttleTime);
        	
        	bodyAcclAngle = Math.toDegrees(Math.atan((groundVelocity/forceOfInertia)*-0.01)); 
        	
        	return bodyAcclAngle;
        	
        }else if(throttleTime > 10 && !parkingBrakeOn && groundVelocity <= -3){
        	
        	bodyAcceleration = (groundVelocity/throttleTime);
        	
        	bodyAcclAngle = Math.toDegrees(Math.atan((groundVelocity/forceOfInertia)*0.01)); 
        	
        	return bodyAcclAngle;
        	
        }else {
        	
        	bodyAcclAngle = 0;
        	
        	return 0;
        }
     }
	
	//Braking animation which calculates the amount of time the brake is pressed to derive the deceleration
	//to then get the value of force exerted by inertia.
	public double brakeInertia() {
		
		forceOfInertia = currentMass*(bodyAcceleration);
		
	    if (brakeOn && velocity != 0 || parkingBrakeOn && velocity != 0) {
	    	
	    	bodyAcceleration = (groundVelocity/brakingTime);
	    	
	        if(groundVelocity < -3) {
	        	
	        	bodyBrakeAngle = Math.toDegrees(Math.atan((groundVelocity/forceOfInertia)*-0.01));
	        	
	        }else {
	        	
		        bodyBrakeAngle = Math.toDegrees(Math.atan((groundVelocity/forceOfInertia)*0.01));
		        
	        }
	        
	        return bodyBrakeAngle;
	        
	    }else {
	    	
	    	bodyBrakeAngle = 0;
	    	
	    	return 0;
	    }
	 }
	
	/**
	 *  This needs to be called before checking pitch and roll of ground devices.  It is responsible for ensuring that
	 *  the Y position of the vehicle is in such a place that the checks can run.  If motionY is negative, and opposite
	 *  ground devices are collided it will add motionY to a level that the least-collided ground device will no longer collide.
	 *  If motionY is positive, then it will add motionY to prevent either the front or rear ground devices from colliding, but only
	 *  if motionPitch is non-zero.  This is for vehicles that rotate when flying and need to pivot on their ground devices.
	 *  Finally, if motionY is negative, and only the rear ground devices are collided, but motionPitch is also negative, it will
	 *  add motionY to get those out of the ground.  This is also for flying vehicles, in that they normally pitch down (positive)
	 *  due to gravity, but if they are trying to pitch up we need to allow them to do so.
	 *  
	 *  In all cases, this function will return the amount of motionY boost that was applied.  This value should be removed
	 *  from motionY after the vehicle is moved, as it's not a force and will cause the physics system to behave badly if
	 *  it is left in.  The only exception is if we had motionY that we shouldn't have applied in the first place, such as
	 *  if motionY was moving us into the ground.  In this case, we need to keep this as a force as it's force the ground is
	 *  applying to the vehicle to keep it from moving into the ground (normal force).
	 */
	private double correctMotionYMovement(){
		if(motion.y < 0){
			if(rotation.x >= -0.1){
				if((frontLeftGroundDeviceBox.isCollided && rearRightGroundDeviceBox.isCollided) || (frontRightGroundDeviceBox.isCollided && rearLeftGroundDeviceBox.isCollided)){
					double collisionDepth = Math.max(Math.min(frontLeftGroundDeviceBox.collisionDepth, rearRightGroundDeviceBox.collisionDepth), Math.min(frontRightGroundDeviceBox.collisionDepth, rearLeftGroundDeviceBox.collisionDepth));
					double motionToNotCollide = collisionDepth/SPEED_FACTOR;
					motion.y += motionToNotCollide;
					//Check if motionY is close to 0.  If so, we should make it 0 as we are
					//just sitting on the ground and shouldn't have any motionY to begin with.
					if(Math.abs(motion.y) < 0.0001){
						motion.y = 0;
					}
					
					frontLeftGroundDeviceBox.updateCollisionStatuses();
					frontRightGroundDeviceBox.updateCollisionStatuses();
					rearLeftGroundDeviceBox.updateCollisionStatuses();
					rearRightGroundDeviceBox.updateCollisionStatuses();
					return motion.y > 0 ? motion.y : 0;
				}
			}else{
				if(rearLeftGroundDeviceBox.isCollided || rearRightGroundDeviceBox.isCollided){
					double collisionDepth = Math.max(rearLeftGroundDeviceBox.collisionDepth, rearRightGroundDeviceBox.collisionDepth);
					double motionToNotCollide = collisionDepth/SPEED_FACTOR;
					motion.y += motionToNotCollide;
					frontLeftGroundDeviceBox.updateCollisionStatuses();
					frontRightGroundDeviceBox.updateCollisionStatuses();
					rearLeftGroundDeviceBox.updateCollisionStatuses();
					rearRightGroundDeviceBox.updateCollisionStatuses();
					return motion.y;
				}
			}
		}else if(motion.y > 0){
			if(rotation.x > 0 && (frontLeftGroundDeviceBox.isCollided || frontRightGroundDeviceBox.isCollided)){
				double collisionDepth = Math.max(frontLeftGroundDeviceBox.collisionDepth, frontRightGroundDeviceBox.collisionDepth);
				double motionToNotCollide = collisionDepth/SPEED_FACTOR;
				motion.y += motionToNotCollide;
				frontLeftGroundDeviceBox.updateCollisionStatuses();
				frontRightGroundDeviceBox.updateCollisionStatuses();
				rearLeftGroundDeviceBox.updateCollisionStatuses();
				rearRightGroundDeviceBox.updateCollisionStatuses();
				return motion.y;
			}else if(rotation.x < 0 && (rearLeftGroundDeviceBox.isCollided || rearRightGroundDeviceBox.isCollided)){
				double collisionDepth = Math.max(rearLeftGroundDeviceBox.collisionDepth, rearRightGroundDeviceBox.collisionDepth);
				double motionToNotCollide = collisionDepth/SPEED_FACTOR;
				motion.y += motionToNotCollide;
				frontLeftGroundDeviceBox.updateCollisionStatuses();
				frontRightGroundDeviceBox.updateCollisionStatuses();
				rearLeftGroundDeviceBox.updateCollisionStatuses();
				rearRightGroundDeviceBox.updateCollisionStatuses();
				return motion.y;
			}
		}
		return 0;
	}
	
	/**
	 *  If a collision box collided, we need to restrict our proposed movement.
	 *  Do this by removing motions that cause collisions.
	 *  If the motion has a value of 0, skip it as it couldn't have caused the collision.
	 *  Note that even though motionY may have been adjusted for ground device operation prior to this call,
	 *  we shouldn't have an issue with the change as this logic takes priority over that logic to ensure 
	 *  no collision box collides with another block, even if it requires all the ground devices to be collided.
	 */
	private void correctCollidingMovement(){
		rotation.set(0D, 0D, 0D);
		
		//First check the X-axis.
		if(motion.x != 0){
			for(BoundingBox box : collisionBoxes){
				double collisionDepth = getCollisionForAxis(box, true, false, false);
				if(collisionDepth == -1){
					return;
				}else{
					if(motion.x > 0){
						motion.x = Math.max(motion.x - collisionDepth/SPEED_FACTOR, 0);
					}else if(motion.x < 0){
						motion.x = Math.min(motion.x + collisionDepth/SPEED_FACTOR, 0);
					}
				}
			}
		}
		
		//Do the same for the Z-axis
		if(motion.z != 0){
			for(BoundingBox box : collisionBoxes){
				double collisionDepth = getCollisionForAxis(box, false, false, true);
				if(collisionDepth == -1){
					return;
				}else{
					if(motion.z > 0){
						motion.z = Math.max(motion.z - collisionDepth/SPEED_FACTOR, 0);
					}else if(motion.z < 0){
						motion.z = Math.min(motion.z + collisionDepth/SPEED_FACTOR, 0);
					}
				}
			}
		}
		
		//Now that the XZ motion has been limited based on collision we can move in the Y.
		if(motion.y != 0){
			for(BoundingBox box : collisionBoxes){
				double collisionDepth = getCollisionForAxis(box, false, true, false);
				if(collisionDepth == -1){
					return;
				}else if(collisionDepth != 0){
					if(motion.y > 0){
						motion.y = Math.max(motion.y - collisionDepth/SPEED_FACTOR, 0);
					}else if(motion.y < 0){
						motion.y = Math.min(motion.y + collisionDepth/SPEED_FACTOR, 0);
					}
				}
			}
		}
		
		//Check the yaw.
		if(rotation.y != 0){
			tempBoxAngles.set(0D, rotation.y, 0D).add(angles);
			for(BoundingBox box : collisionBoxes){
				while(rotation.y != 0){
					tempBoxPosition.setTo(box.localCenter).rotateCoarse(tempBoxAngles).add(position).add(motion.x*SPEED_FACTOR, motion.y*SPEED_FACTOR, motion.z*SPEED_FACTOR);
					//Raise this box ever so slightly because Floating Point errors are a PITA.
					tempBoxPosition.add(0D, 0.1D, 0D);
					if(!box.updateCollidingBlocks(world, tempBoxPosition.subtract(box.globalCenter))){
						break;
					}
					if(rotation.y > 0){
						rotation.y = Math.max(rotation.y - 0.1F, 0);
					}else{
						rotation.y = Math.min(rotation.y + 0.1F, 0);
					}
				}
			}
		}

		//Now do pitch.
		//Make sure to take into account yaw as it's already been checked.
		if(rotation.x != 0){
			tempBoxAngles.set(rotation.x, rotation.y, 0D).add(angles);
			for(BoundingBox box : collisionBoxes){
				while(rotation.x != 0){
					tempBoxPosition.setTo(box.localCenter).rotateCoarse(tempBoxAngles).add(position).add(motion.x*SPEED_FACTOR, motion.y*SPEED_FACTOR, motion.z*SPEED_FACTOR);
					if(!box.updateCollidingBlocks(world, tempBoxPosition.subtract(box.globalCenter))){
						break;
					}
					if(rotation.x > 0){
						rotation.x = Math.max(rotation.x - 0.1F, 0);
					}else{
						rotation.x = Math.min(rotation.x + 0.1F, 0);
					}
				}
			}
		}
		
		//And lastly the roll.
		if(rotation.z != 0){
			tempBoxAngles.setTo(rotation).add(angles);
			for(BoundingBox box : collisionBoxes){
				while(rotation.z != 0){
					tempBoxPosition.setTo(box.localCenter).rotateCoarse(tempBoxAngles).add(position).add(motion.x*SPEED_FACTOR, motion.y*SPEED_FACTOR, motion.z*SPEED_FACTOR);
					if(!box.updateCollidingBlocks(world, tempBoxPosition.subtract(box.globalCenter))){
						break;
					}
					if(rotation.z > 0){
						rotation.z = Math.max(rotation.z - 0.1F, 0);
					}else{
						rotation.z = Math.min(rotation.z + 0.1F, 0);
					}
				}
			}
		}
		
		//As a final precaution, take a bit of extra movement off of the motions.
		//This is done to prevent vehicles from moving into blocks when they shouldn't
		//due to floating-point errors.
		if(motion.x > 0){
			motion.x -= 0.002F;
		}
		if(motion.x < 0){
			motion.x += 0.002F;
		}
		if(motion.z > 0){
			motion.z -= 0.002F;
		}
		if(motion.z < 0){
			motion.z += 0.002F;
		}
	}

	/**
	 *  Called to adjust the pitch of the vehicle to handle collided ground devices.
	 *  This adds to motionPitch to the vehicle, as well as returns a value to compensate for the motionPitch added.
	 *  This value should be added to motionY when applying vehicle motion, but should not be left in as it's
	 *  not a force and will cause the physics system to mis-behave.
	 *  
	 *  This system works by checking which ground devices are collided (if any) and rotating the vehicle accordingly.
	 *  Note that the yCoord for the boxes is their center, so we need to take half the height to get the
	 *  collision point at the ground for all ground devices.  We limit rotation in all cases to
	 *  2 degrees per tick, which is 40 degrees a second.  Plenty fast, and prevents vehicles from
	 *  instantly pitching up on steep slopes.  This isn't Big Rigs-Over the Road Racing...
	 */
	private double correctPitchMovement(){
		//If we only have front ground devices collided, we need to pitch up.
		//If we only have rear ground devices collided, we need to pitch down.
		//In either case, we will have to rotate the vehicle and move it in the Y-direction.
		//This is to ensure it follows the ground after pitching rather than pitching in the air.
		double ptichRotationBoost = 0;
		if((frontLeftGroundDeviceBox.collisionDepth > 0 || frontRightGroundDeviceBox.collisionDepth > 0) && rearLeftGroundDeviceBox.collisionDepth == 0 && rearRightGroundDeviceBox.collisionDepth == 0){				
			//First, we get the front point that has collided.
			//This is needed no matter if the rears are grounded or not.
			double frontY;
			double frontZ;
			double collisionDepth;
			if(frontLeftGroundDeviceBox.collisionDepth > frontRightGroundDeviceBox.collisionDepth){
				frontY = frontLeftGroundDeviceBox.contactPoint.y;
				frontZ = frontLeftGroundDeviceBox.contactPoint.z;
				collisionDepth = frontLeftGroundDeviceBox.collisionDepth;
			}else{
				frontY = frontRightGroundDeviceBox.contactPoint.y;
				frontZ = frontRightGroundDeviceBox.contactPoint.z;
				collisionDepth = frontRightGroundDeviceBox.collisionDepth;
			}
			
			if(rearLeftGroundDeviceBox.isGrounded || rearRightGroundDeviceBox.isGrounded){
				//Get the farthest-back grounded rear point for the greatest angle.
				double rearY;
				double rearZ;
				if(rearLeftGroundDeviceBox.isGrounded && rearRightGroundDeviceBox.isGrounded){
					if(rearLeftGroundDeviceBox.contactPoint.z < rearRightGroundDeviceBox.contactPoint.z){
						rearY = rearLeftGroundDeviceBox.contactPoint.y;
						rearZ = rearLeftGroundDeviceBox.contactPoint.z;
					}else{
						rearY = rearRightGroundDeviceBox.contactPoint.y;
						rearZ = rearRightGroundDeviceBox.contactPoint.z;
					}
				}else if(rearLeftGroundDeviceBox.isGrounded){
					rearY = rearLeftGroundDeviceBox.contactPoint.y;
					rearZ = rearLeftGroundDeviceBox.contactPoint.z;
				}else{
					rearY = rearRightGroundDeviceBox.contactPoint.y;
					rearZ = rearRightGroundDeviceBox.contactPoint.z;
				}

				//Finally, get the distance between the two points and the angle needed to get out of the collision.
				//After that, calculate how much we will need to offset the vehicle to keep the rear in the same place.
				double distance = Math.hypot(frontY - rearY, frontZ - rearZ);
				double angle = -Math.min(Math.asin(Math.min(collisionDepth/distance, 1)), MAX_ROTATION_RAD_PER_TICK);
				rotation.x += Math.toDegrees(angle);
				ptichRotationBoost = -Math.sin(angle)*Math.hypot(rearY, rearZ);
			}else{
				//In this case, we are just trying to get to a point where we have a grounded ground device.
				//This will allow us to rotate about it and level the vehicle.
				//We just rotate as much as we can here, and if we have negative motionY we need to set it to 0
				//after this cycle.  This is because we don't want to go down any further than we are until we can
				//do calcs using the grounded ground device.  Use the collision variable we use for the motionY
				//as it won't be used at this point as one of the sets of ground devices are free.  If it was used
				//because we were in the ground then one set would have to be grounded.
				double angle = -MAX_ROTATION_RAD_PER_TICK;
				rotation.x += Math.toDegrees(angle);
				if(motion.y < 0){
					ptichRotationBoost = Math.sin(angle)*Math.hypot(frontY, frontZ) + motion.y*SPEED_FACTOR;
					motion.y = 0;
				}else{
					ptichRotationBoost = Math.sin(angle)*Math.hypot(frontY, frontZ);
				}
			}
		}else if((rearLeftGroundDeviceBox.collisionDepth > 0 || rearRightGroundDeviceBox.collisionDepth > 0) && frontLeftGroundDeviceBox.collisionDepth == 0 && frontRightGroundDeviceBox.collisionDepth == 0){				
			//First, we get the rear point that has collided.
			//This is needed no matter if the fronts are grounded or not.
			double rearY;
			double rearZ;
			double collisionDepth;
			if(rearLeftGroundDeviceBox.collisionDepth > rearRightGroundDeviceBox.collisionDepth){
				rearY = rearLeftGroundDeviceBox.contactPoint.y;
				rearZ = rearLeftGroundDeviceBox.contactPoint.z;
				collisionDepth = rearLeftGroundDeviceBox.collisionDepth;
			}else{
				rearY = rearRightGroundDeviceBox.contactPoint.y;
				rearZ = rearRightGroundDeviceBox.contactPoint.z;
				collisionDepth = rearRightGroundDeviceBox.collisionDepth;
			}
			
			if(frontLeftGroundDeviceBox.isGrounded || frontRightGroundDeviceBox.isGrounded){
				//Get the farthest-forward grounded front point for the greatest angle.
				double frontY;
				double frontZ;
				if(frontLeftGroundDeviceBox.isGrounded && frontRightGroundDeviceBox.isGrounded){
					if(frontLeftGroundDeviceBox.contactPoint.z > frontRightGroundDeviceBox.contactPoint.z){
						frontY = frontLeftGroundDeviceBox.contactPoint.y;
						frontZ = frontLeftGroundDeviceBox.contactPoint.z;
					}else{
						frontY = frontRightGroundDeviceBox.contactPoint.y;
						frontZ = frontRightGroundDeviceBox.contactPoint.z;
					}
				}else if(frontLeftGroundDeviceBox.isGrounded){
					frontY = frontLeftGroundDeviceBox.contactPoint.y;
					frontZ = frontLeftGroundDeviceBox.contactPoint.z;
				}else{
					frontY = frontRightGroundDeviceBox.contactPoint.y;
					frontZ = frontRightGroundDeviceBox.contactPoint.z;
				}

				//Finally, get the distance between the two points and the angle needed to get out of the collision.
				//After that, calculate how much we will need to offset the vehicle to keep the front in the same place.
				double distance = Math.hypot(frontY - rearY, frontZ - rearZ);
				double angle = Math.min(Math.asin(Math.min(collisionDepth/distance, 1)), MAX_ROTATION_RAD_PER_TICK);
				rotation.x += Math.toDegrees(angle);
				ptichRotationBoost = Math.sin(angle)*Math.hypot(frontY, frontZ);
			}else{
				//In this case, we are just trying to get to a point where we have a grounded ground device.
				//This will allow us to rotate about it and level the vehicle.
				//We just rotate as much as we can here, and if we have negative motionY we need to set it to 0
				//after this cycle.  This is because we don't want to go down any further than we are until we can
				//do calcs using the grounded ground device.  Use the collision variable we use for the motionY
				//as it won't be used at this point as one of the sets of ground devices are free.  If it was used
				//because we were in the ground then one set would have to be grounded.
				double angle = MAX_ROTATION_RAD_PER_TICK;
				rotation.x += Math.toDegrees(angle);
				if(motion.y < 0){
					ptichRotationBoost = -Math.sin(angle)*Math.hypot(rearY, rearZ) + motion.y*SPEED_FACTOR;
					motion.y = 0;
				}else{
					ptichRotationBoost = -Math.sin(angle)*Math.hypot(rearY, rearZ);
				}
			}
		}
		return ptichRotationBoost;
	}

	/**
	 *  Called to adjust the roll of the vehicle to handle collided ground devices.
	 *  Same as correctPitchMovment, just for roll using the X axis rather than Z.
	 */
	private double correctRollMovement(){
		//Negative rolls left, postive rolls right.
		//If we only have left ground devices collided, we need to roll left.
		//If we only have right ground devices collided, we need to roll right.
		//In either case, we will have to rotate the vehicle and move it in the Y-direction.
		//This is to ensure it follows the ground after rolling rather than rolling in the air.
		double rollRotationBoost = 0;
		if((frontRightGroundDeviceBox.collisionDepth > 0 || rearRightGroundDeviceBox.collisionDepth > 0) && frontLeftGroundDeviceBox.collisionDepth == 0 && rearLeftGroundDeviceBox.collisionDepth == 0){				
			//Negative "pitch" added here, so we need to have an operation that adds roll to the left.
			//This is if we have the right side collided.
			//Swap front with right and rear with left.
			//First, we get the right point that has collided.
			//This is needed no matter if the lefts are grounded or not.
			double rightY;
			double rightX;
			double collisionDepth;
			if(frontRightGroundDeviceBox.collisionDepth > rearRightGroundDeviceBox.collisionDepth){
				rightY = frontRightGroundDeviceBox.contactPoint.y;
				rightX = frontRightGroundDeviceBox.contactPoint.x;
				collisionDepth = frontRightGroundDeviceBox.collisionDepth;
			}else{
				rightY = rearRightGroundDeviceBox.contactPoint.y;
				rightX = rearRightGroundDeviceBox.contactPoint.x;
				collisionDepth = rearRightGroundDeviceBox.collisionDepth;
			}
			
			if(frontLeftGroundDeviceBox.isGrounded || rearLeftGroundDeviceBox.isGrounded){
				//Get the farthest-left grounded left point for the greatest angle.
				double leftY;
				double leftX;
				if(frontLeftGroundDeviceBox.isGrounded && rearLeftGroundDeviceBox.isGrounded){
					if(frontLeftGroundDeviceBox.contactPoint.x < rearLeftGroundDeviceBox.contactPoint.x){
						leftY = frontLeftGroundDeviceBox.contactPoint.y;
						leftX = frontLeftGroundDeviceBox.contactPoint.x;
					}else{
						leftY = rearLeftGroundDeviceBox.contactPoint.y;
						leftX = rearLeftGroundDeviceBox.contactPoint.x;
					}
				}else if(frontLeftGroundDeviceBox.isGrounded){
					leftY = frontLeftGroundDeviceBox.contactPoint.y;
					leftX = frontLeftGroundDeviceBox.contactPoint.x;
				}else{
					leftY = rearLeftGroundDeviceBox.contactPoint.y;
					leftX = rearLeftGroundDeviceBox.contactPoint.x;
				}

				//Finally, get the distance between the two points and the angle needed to get out of the collision.
				//After that, calculate how much we will need to offset the vehicle to keep the rear in the same place.
				double distance = Math.hypot(rightY - leftY, rightX - leftX);
				double angle = -Math.min(Math.asin(Math.min(collisionDepth/distance, 1)), MAX_ROTATION_RAD_PER_TICK);
				rotation.z += Math.toDegrees(angle);
				rollRotationBoost = -Math.sin(angle)*Math.hypot(leftY, leftX);
			}else{
				//In this case, we are just trying to get to a point where we have a grounded ground device.
				//This will allow us to rotate about it and level the vehicle.
				//We just rotate as much as we can here, and if we have negative motionY we need to set it to 0
				//after this cycle.  This is because we don't want to go down any further than we are until we can
				//do calcs using the grounded ground device.  Use the collision variable we use for the motionY
				//as it won't be used at this point as one of the sets of ground devices are free.  If it was used
				//because we were in the ground then one set would have to be grounded.
				double angle = -MAX_ROTATION_RAD_PER_TICK;
				rotation.z += Math.toDegrees(angle);
				if(motion.y < 0){
					rollRotationBoost = Math.sin(angle)*Math.hypot(rightY, rightX) + motion.y*SPEED_FACTOR;
					motion.y = 0;
				}else{
					rollRotationBoost = Math.sin(angle)*Math.hypot(rightY, rightX);
				}
			}
		}else if((frontLeftGroundDeviceBox.collisionDepth > 0 || rearLeftGroundDeviceBox.collisionDepth > 0) && frontRightGroundDeviceBox.collisionDepth == 0 && rearRightGroundDeviceBox.collisionDepth == 0){				
			//First, we get the left point that has collided.
			//This is needed no matter if the rights are grounded or not.
			double leftY;
			double leftX;
			double collisionDepth;
			if(frontLeftGroundDeviceBox.collisionDepth > rearLeftGroundDeviceBox.collisionDepth){
				leftY = frontLeftGroundDeviceBox.contactPoint.y;
				leftX = frontLeftGroundDeviceBox.contactPoint.x;
				collisionDepth = frontLeftGroundDeviceBox.collisionDepth;
			}else{
				leftY = rearLeftGroundDeviceBox.contactPoint.y;
				leftX = rearLeftGroundDeviceBox.contactPoint.x;
				collisionDepth = rearLeftGroundDeviceBox.collisionDepth;
			}
			
			if(frontRightGroundDeviceBox.isGrounded || rearRightGroundDeviceBox.isGrounded){
				//Get the farthest-right grounded front point for the greatest angle.
				double rightY;
				double rightX;
				if(frontRightGroundDeviceBox.isGrounded && rearRightGroundDeviceBox.isGrounded){
					if(frontRightGroundDeviceBox.contactPoint.x > rearRightGroundDeviceBox.contactPoint.x){
						rightY = frontRightGroundDeviceBox.contactPoint.y;
						rightX = frontRightGroundDeviceBox.contactPoint.x;
					}else{
						rightY = rearRightGroundDeviceBox.contactPoint.y;
						rightX = rearRightGroundDeviceBox.contactPoint.x;
					}
				}else if(frontRightGroundDeviceBox.isGrounded){
					rightY = frontRightGroundDeviceBox.contactPoint.y;
					rightX = frontRightGroundDeviceBox.contactPoint.x;
				}else{
					rightY = rearRightGroundDeviceBox.contactPoint.y;
					rightX = rearRightGroundDeviceBox.contactPoint.x;
				}

				//Finally, get the distance between the two points and the angle needed to get out of the collision.
				//After that, calculate how much we will need to offset the vehicle to keep the right in the same place.
				double distance = Math.hypot(rightY - leftY, rightX - leftX);
				double angle = Math.min(Math.asin(Math.min(collisionDepth/distance, 1)), MAX_ROTATION_RAD_PER_TICK);
				rotation.z += Math.toDegrees(angle);
				rollRotationBoost = Math.sin(angle)*Math.hypot(rightY, rightX);
			}else{
				//In this case, we are just trying to get to a point where we have a grounded ground device.
				//This will allow us to rotate about it and level the vehicle.
				//We just rotate as much as we can here, and if we have negative motionY we need to set it to 0
				//after this cycle.  This is because we don't want to go down any further than we are until we can
				//do calcs using the grounded ground device.  Use the collision variable we use for the motionY
				//as it won't be used at this point as one of the sets of ground devices are free.  If it was used
				//because we were in the ground then one set would have to be grounded.
				double angle = MAX_ROTATION_RAD_PER_TICK;
				rotation.z += Math.toDegrees(angle);
				if(motion.y < 0){
					rollRotationBoost = -Math.sin(angle)*Math.hypot(leftY, leftX) + motion.y*SPEED_FACTOR;
					motion.y = 0;
				}else{
					rollRotationBoost = -Math.sin(angle)*Math.hypot(leftY, leftX);
				}
			}
		}
		return rollRotationBoost;
	}

	
	/**
	 * Call this when moving vehicle to ensure they move correctly.
	 * Failure to do this will result in things going badly!
	 */
	private void moveVehicle(){
		//First, populate the variables for ground and collided states for the groundDevices.
		frontLeftGroundDeviceBox.updateCollisionStatuses();
		frontRightGroundDeviceBox.updateCollisionStatuses();
		rearLeftGroundDeviceBox.updateCollisionStatuses();
		rearRightGroundDeviceBox.updateCollisionStatuses();
		
		//Now check to make sure our ground devices are in the right spot relative to the ground.
		double groundCollisionBoost = correctMotionYMovement();
		
		//After checking the ground devices to ensure we aren't shoving ourselves into the ground, we try to move the vehicle.
		//If the vehicle can move without a collision box colliding with something, then we can move to the re-positioning of the vehicle.
		//That is done through trig functions.  If we hit something, however, we need to inhibit the movement so we don't do that.
		boolean collisionBoxCollided = false;
		tempBoxAngles.setTo(rotation).add(angles);
		for(BoundingBox box : collisionBoxes){
			tempBoxPosition.setTo(box.localCenter).rotateCoarse(tempBoxAngles).add(position).add(motion.x*SPEED_FACTOR, motion.y*SPEED_FACTOR, motion.z*SPEED_FACTOR);
			if(box.updateCollidingBlocks(world, tempBoxPosition.subtract(box.globalCenter))){
				collisionBoxCollided = true;
				break;
			}
		}
		
		//Handle collision box collisions, if we have any.
		//This is done after the ground device logic, as that removes any excess motionY caused by gravity.
		//The gravity could cause more collisions here that really don't need to happen, and we don't want
		//to check those as the checks for collision box collision are far more intensive than the ones for ground devices.
		//If we have collisions, handle those rather than do ground device operations, otherwise do ground device operations as normal.
		double groundRotationBoost = 0;
		if(collisionBoxCollided){
			correctCollidingMovement();
		}else{
			//If we have pitch corrections boost on those.  Otherwise boost on roll corrections.
			//We don't want to do both pitch and roll the same tick as they could conflict with each other.
			double pitchRotationBoost = correctPitchMovement();
			if(pitchRotationBoost != 0){
				groundRotationBoost = pitchRotationBoost;
				motion.y += groundRotationBoost/SPEED_FACTOR;
			}else{
				groundRotationBoost = correctRollMovement();
				motion.y += groundRotationBoost/SPEED_FACTOR;
			}
		}

		//Now that that the movement has been checked, move the vehicle.
		Point3d factoredMotion = motion.copy().multiply(SPEED_FACTOR);
		if(!world.isClient()){
			if(!motion.isZero() || !rotation.isZero()){
				position.add(factoredMotion);
				angles.add(rotation);
				addToServerDeltas(factoredMotion, rotation);
				InterfaceNetwork.sendToClientsTracking(new PacketVehicleServerMovement((EntityVehicleF_Physics) this, factoredMotion, rotation), this);
			}
		}else{
			//Make sure the server is sending delta packets and NBT is initialized before we try to do delta correction.
			if(!serverDeltaM.isZero()){
				//Check to make sure the delta is non-zero before trying to do complex math to calculate it.
				//Saves a bit of CPU power due to division and multiplication operations, and prevents constant
				//movement due to floating-point errors.
				if(serverDeltaM.x - clientDeltaM.x != 0){
					clientMApplied.x = factoredMotion.x + (serverDeltaM.x - clientDeltaM.x)/25D*Math.abs(serverDeltaM.x - clientDeltaM.x);
				}else{
					clientMApplied.x = 0;
				}
				if(serverDeltaM.y - clientDeltaM.y != 0){
					clientMApplied.y = factoredMotion.y + (serverDeltaM.y - clientDeltaM.y)/25D*Math.abs(serverDeltaM.y - clientDeltaM.y);
				}else{
					clientMApplied.y = 0;
				}
				if(serverDeltaM.z - clientDeltaM.z != 0){
					clientMApplied.z = factoredMotion.z + (serverDeltaM.z - clientDeltaM.z)/25D*Math.abs(serverDeltaM.z - clientDeltaM.z);
				}else{
					clientMApplied.z = 0;
				}
				
				if(serverDeltaA.x - clientDeltaA.x != 0){
					clientAApplied.x = rotation.x + (serverDeltaA.x - clientDeltaA.x)/25D*Math.abs(serverDeltaA.x - clientDeltaA.x);
				}else{
					clientAApplied.x = 0;
				}
				if(serverDeltaA.y - clientDeltaA.y != 0){
					clientAApplied.y = rotation.y + (serverDeltaA.y - clientDeltaA.y)/25D*Math.abs(serverDeltaA.y - clientDeltaA.y);
				}else{
					clientAApplied.y = 0;
				}
				if(serverDeltaA.z - clientDeltaA.z != 0){
					clientAApplied.z = rotation.z + (serverDeltaA.z - clientDeltaA.z)/25D*Math.abs(serverDeltaA.z - clientDeltaA.z);
				}else{
					clientAApplied.z = 0;
				}
				
				position.add(clientMApplied);
				angles.add(clientAApplied);
				clientDeltaM.add(clientMApplied);
				clientDeltaA.add(clientAApplied);
			}else{
				position.add(factoredMotion);
				angles.add(rotation);
			}
		}
		
		//After all movement is done, try and move players on hitboxes.
		//Note that we need to interpolate the delta here based on actual movement, so don't use the vehicle motion!
		if(velocity != 0){
			//FIXME disabled for now.
			/*
			for(EntityPlayer player : world.playerEntities){
				if(!this.equals(player.getRidingEntity())){
					for(BoundingBox box : collisionBoxes){
						//Add a slight yOffset to every box to "grab" players standing on collision points.
						if(box.offset(posX - prevPosX, posY - prevPosY + 0.1F, posZ - prevPosZ).intersects(player.getEntityBoundingBox())){
							//Player has collided with this vehicle.  Adjust movement to allow them to ride on it.
							//If we are going too fast, the player should slip off the collision box if it's not an interior box.
							if(velocity <= ConfigSystem.configObject.general.clingSpeed.value || box.isInterior){
								player.setPosition(player.posX + (posX - prevPosX), player.posY + (posY - prevPosY), player.posZ + (posZ - prevPosZ));
							}else if(velocity < 2F*ConfigSystem.configObject.general.clingSpeed.value){
								double slip = (2F*ConfigSystem.configObject.general.clingSpeed.value - velocity)*4D;
								player.setPosition(player.posX + (posX - prevPosX)*slip, player.posY + (posY - prevPosY)*slip, player.posZ + (posZ - prevPosZ)*slip);
							}
							break;
						}
					}
				}
			}*/
		}
		
		//Before we end this tick we need to remove any motions added for ground devices.  These motions are required 
		//only for the updating of the vehicle position due to rotation operations and should not be considered forces.
		//Leaving them in will cause the physics system to think a force was applied, which will make it behave badly!
		//We need to strip away any positive motionY we gave the vehicle to get it out of the ground if it
		//collided on its ground devices, as well as any motionY we added when doing rotation adjustments.
		motion.y -= (groundCollisionBoost + groundRotationBoost/SPEED_FACTOR);
	}
	
	/**
	 * Method block for ground operations.  This does braking force
	 * and turning for applications independent of vehicle-specific
	 * movement.  Must come AFTER force calculations as it depends on motions.
	 */
	private void performGroundOperations(){
		float brakingFactor = getBrakingForceFactor();
		if(brakingFactor > 0){
			double motionXBraking = 20F*brakingFactor/currentMass*Math.signum(motion.x);
			double motionZBraking = 20F*brakingFactor/currentMass*Math.signum(motion.z);
			//If we have more braking than motions, just set our velocity to 0.
			if(motion.x*motion.x + motion.z*motion.z < motionXBraking*motionXBraking + motionZBraking*motionZBraking){
				motion.x = 0;
				motion.z = 0;
				rotation.y = 0;
			}else{
				motion.x -= motionXBraking;
				motion.z -= motionZBraking;
			}
		}
		
		float skiddingFactor = getSkiddingFactor();
		if(skiddingFactor != 0){
			Point3d groundHeading = new Point3d(headingVector.x, 0, headingVector.z).normalize();
			double vectorDelta = groundVelocityVector.copy().normalize().distanceTo(groundHeading);
			if(vectorDelta > 0.001){
				vectorDelta = Math.min(skiddingFactor, vectorDelta);
				rotation.y += vectorDelta;
				motion.x = groundHeading.x * Math.abs(groundVelocity);
				motion.z = groundHeading.z * Math.abs(groundVelocity);
			}
		}
		
		rotation.y += getTurningFactor();
	}
	
	/**
	 * Returns factor for braking.
	 * Depends on number of grounded core collision sections and braking ground devices.
	 */
	private float getBrakingForceFactor(){
		float brakingFactor = 0;
		//First get the ground device braking contributions.
		//This is both grounded ground devices, and liquid collision boxes that are set as such.
		for(PartGroundDevice groundDevice : this.groundedGroundDevices){
			float addedFactor = 0;
			if(brakeOn || parkingBrakeOn){
				addedFactor = groundDevice.getMotiveFriction();
				if(velocity > 0) {
					brakingTime++;
				}
			}else if(velocity == 0) {
				brakingTime = 10;
			}
			if(addedFactor != 0){
				brakingFactor += Math.max(addedFactor - groundDevice.getFrictionLoss(), 0);
			}
		}
		if(brakeOn || parkingBrakeOn){
			if(frontLeftGroundDeviceBox.isCollidedLiquid || frontLeftGroundDeviceBox.isGroundedLiquid)brakingFactor += 0.5;
			if(frontRightGroundDeviceBox.isCollidedLiquid || frontRightGroundDeviceBox.isGroundedLiquid)brakingFactor += 0.5;
			if(rearLeftGroundDeviceBox.isCollidedLiquid || rearLeftGroundDeviceBox.isGroundedLiquid)brakingFactor += 0.5;
			if(rearRightGroundDeviceBox.isCollidedLiquid || rearRightGroundDeviceBox.isGroundedLiquid)brakingFactor += 0.5;
		}
		
		//Now get any contributions from the colliding collision bits.
		for(BoundingBox box : collisionBoxes){
			if(!box.collidingBlocks.isEmpty()){
				Point3i groundPosition = new Point3i(box.globalCenter);
				WrapperBlock groundBlock = world.getWrapperBlock(groundPosition);
				if(groundBlock != null){
					//0.6 is default slipperiness for blocks.  Anything extra should reduce friction, anything less should increase it.
					float frictionLoss = 0.6F - groundBlock.getSlipperiness() + (groundBlock.isRaining() ? 0.25F : 0);
					brakingFactor += Math.max(2.0 - frictionLoss, 0);
				}
			}
		}
		return brakingFactor;
	}
	
	/**
	 * Returns factor for skidding based on lateral friction and velocity.
	 * If the value is non-zero, it indicates that yaw changes from ground
	 * device calculations should be applied due to said devices being in
	 * contact with the ground.  Note that this should be called prior to 
	 * turning code as it will interpret the yaw change as a skid and will 
	 * attempt to prevent it!
	 */
	private float getSkiddingFactor(){
		float skiddingFactor = 0;
		//First check grounded ground devices.
		for(PartGroundDevice groundDevice : this.groundedGroundDevices){
			skiddingFactor += groundDevice.getLateralFriction() - groundDevice.getFrictionLoss();
		}
		
		//Now check if any collision boxes are in liquid.  Needed for maritime vehicles.
		if(frontLeftGroundDeviceBox.isCollidedLiquid || frontLeftGroundDeviceBox.isGroundedLiquid)skiddingFactor += 0.5;
		if(frontRightGroundDeviceBox.isCollidedLiquid || frontRightGroundDeviceBox.isGroundedLiquid)skiddingFactor += 0.5;
		if(rearLeftGroundDeviceBox.isCollidedLiquid || rearLeftGroundDeviceBox.isGroundedLiquid)skiddingFactor += 0.5;
		if(rearRightGroundDeviceBox.isCollidedLiquid || rearRightGroundDeviceBox.isGroundedLiquid)skiddingFactor += 0.5;
		return skiddingFactor > 0 ? skiddingFactor : 0;
	}
	
	/**
	 * Returns factor for turning based on lateral friction, velocity, and wheel distance.
	 * Sign of returned value indicates which direction entity should yaw.
	 * A 0 value indicates no yaw change.
	 */
	protected float getTurningFactor(){
		float turningForce = 0;
		float steeringAngle = this.getSteerAngle();
		if(steeringAngle != 0){
			float turningFactor = 0;
			float turningDistance = 0;
			//Check grounded wheels for turn contributions.
			for(PartGroundDevice groundDevice : this.groundedGroundDevices){
				float frictionLoss = groundDevice.getFrictionLoss();
				//Do we have enough friction to change yaw?
				if(groundDevice.vehicleDefinition.turnsWithSteer && groundDevice.getLateralFriction() - frictionLoss > 0){
					turningFactor += groundDevice.getLateralFriction() - frictionLoss;
					turningDistance = (float) Math.max(turningDistance, Math.abs(groundDevice.placementOffset.z));
				}
			}
			//Also check for boat engines, which can make us turn if we are in water.
			for(APart part : parts){
				if(part instanceof PartPropeller){
					if(part.isInLiquid()){
						turningFactor += 1.0F;
						turningDistance = (float) Math.max(turningDistance, Math.abs(part.placementOffset.z));
					}
				}
			}
			if(turningFactor > 0){
				//Now that we know we can turn, we can attempt to change the track.
				steeringAngle = Math.abs(steeringAngle);
				if(turningFactor < 1){
					steeringAngle *= turningFactor;
				}
				//Adjust steering angle to be aligned with distance of the turning part from the center of the vehicle.
				steeringAngle /= turningDistance;
				//Another thing that can affect the steering angle is speed.
				//More speed makes for less wheel turn to prevent crazy circles.
				if(groundVelocity*SPEED_FACTOR/0.35F - turningFactor/3F > 0){
					steeringAngle *= Math.pow(0.25F, (groundVelocity*(0.75F + SPEED_FACTOR/0.35F/4F) - turningFactor/3F));
				}
				//Adjust turn force to steer angle based on turning factor.
				turningForce = -(float) (steeringAngle*groundVelocity/2F);
				//Correct for speedFactor changes.
				turningForce *= SPEED_FACTOR/0.35F;
				//Now add the sign to this force.
				turningForce *= Math.signum(this.getSteerAngle());
			}
		}
		return turningForce;
	}
	
	public void addToServerDeltas(Point3d motion, Point3d rotation){
		serverDeltaM.add(motion);
		serverDeltaA.add(rotation);
	}
	
	/**
	 * Method block for force and motion calculations.
	 */
	protected abstract void getForcesAndMotions();
	
	/**
	 * Returns whatever the steering angle is.
	 * Used for rendering and turning force calculations.
	 */
	public abstract float getSteerAngle();
	
	/**
	 * Method block for dampening control surfaces.
	 * Used to move control surfaces back to neutral position.
	 */
	protected abstract void dampenControlSurfaces();
    
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setBoolean("locked", locked);
		data.setBoolean("brakeOn", brakeOn);
		data.setBoolean("parkingBrakeOn", parkingBrakeOn);
		data.setString("ownerUUID", ownerUUID);
		data.setString("displayText", displayText);
		data.setPoint3d("serverDeltaM", serverDeltaA);
		data.setPoint3d("serverDeltaM", serverDeltaM);
	}
}
