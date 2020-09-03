package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mcinterface.InterfaceNetwork;
import mcinterface.WrapperBlock;
import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.baseclasses.VehicleGroundDeviceCollection;
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
	
	//Internal states.
	private final Point3d serverDeltaM;
	private final Point3d serverDeltaR;
	private final Point3d clientDeltaM;
	private final Point3d clientDeltaR;
	private final Point3d clientDeltaMApplied = new Point3d(0D, 0D, 0D);
	private final Point3d clientDeltaRApplied = new Point3d(0D, 0D, 0D);
	private final Point3d motionApplied = new Point3d(0D, 0D, 0D);
	private final Point3d rotationApplied = new Point3d(0D, 0D, 0D);
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
	
    //Class used for ground device collisions.
  	protected final VehicleGroundDeviceCollection groundDeviceBoxes;
    
	/**List of ground devices on the ground.  Populated after each movement to be used in turning/braking calculations.*/
	protected final List<PartGroundDevice> groundedGroundDevices = new ArrayList<PartGroundDevice>();
	
	
	public EntityVehicleD_Moving(WrapperWorld world, WrapperNBT data){
		super(world, data);
		this.locked = data.getBoolean("locked");
		this.parkingBrakeOn = data.getBoolean("parkingBrakeOn");
		this.brakeOn = data.getBoolean("brakeOn");
		this.ownerUUID = data.getString("ownerUUID");
		this.serverDeltaM = data.getPoint3d("serverDeltaM");
		this.serverDeltaR = data.getPoint3d("serverDeltaA");
		this.clientDeltaM = serverDeltaM.copy();
		this.clientDeltaR = serverDeltaR.copy();
		this.groundDeviceBoxes = new VehicleGroundDeviceCollection((EntityVehicleF_Physics) this);
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
		
		//Update our GDB members if any of our ground devices don't have the same total offset as placement.
		//This is required to move the GDBs if the GDs move.
		for(APart part : parts){
			if(part instanceof PartGroundDevice){
				if(!part.placementOffset.equals(part.totalOffset)){
					groundDeviceBoxes.updateBounds();
					break;
				}
			}
		}
		
		//Now do update calculations and logic.
		getForcesAndMotions();
		performGroundOperations();
		moveVehicle();
		if(!world.isClient()){
			dampenControlSurfaces();
		}
	}
	
	@Override
	public void addPart(APart part, boolean ignoreCollision){
		super.addPart(part, ignoreCollision);
		groundDeviceBoxes.updateMembers();
		groundDeviceBoxes.updateBounds();
	}
	
	@Override
	public void removePart(APart part, Iterator<APart> iterator){
		super.removePart(part, iterator);
		groundDeviceBoxes.updateMembers();
		groundDeviceBoxes.updateBounds();
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
	/*private double correctMotionYMovement(){
		if(motion.y < 0){
			if(rotation.x >= -0.1){
				if((frontLeftGroundDeviceBox.isCollided && rearRightGroundDeviceBox.isCollided) || (frontRightGroundDeviceBox.isCollided && rearLeftGroundDeviceBox.isCollided)){
					//System.out.println(frontLeftGroundDeviceBox.currentBox.globalCenter.y);
					double collisionDepth = Math.max(Math.min(frontLeftGroundDeviceBox.collisionDepth, rearRightGroundDeviceBox.collisionDepth), Math.min(frontRightGroundDeviceBox.collisionDepth, rearLeftGroundDeviceBox.collisionDepth));
					double motionToNotCollide = collisionDepth/SPEED_FACTOR;
					motion.y += motionToNotCollide;
					//Check if motionY is close to 0.  If so, we should make it 0 as we are
					//just sitting on the ground and shouldn't have any motionY to begin with.
					if(Math.abs(motion.y) < 0.0001){
						//motion.y = 0;
					}
					
					frontLeftGroundDeviceBox.updateCollisions();
					frontRightGroundDeviceBox.updateCollisions();
					rearLeftGroundDeviceBox.updateCollisions();
					rearRightGroundDeviceBox.updateCollisions();
					//FIXME do we need this?
					//return motion.y > 0 ? motion.y : 0;
					return motion.y;
				}
			}else{
				if(rearLeftGroundDeviceBox.isCollided || rearRightGroundDeviceBox.isCollided){
					double collisionDepth = Math.max(rearLeftGroundDeviceBox.collisionDepth, rearRightGroundDeviceBox.collisionDepth);
					double motionToNotCollide = collisionDepth/SPEED_FACTOR;
					motion.y += motionToNotCollide;
					frontLeftGroundDeviceBox.updateCollisions();
					frontRightGroundDeviceBox.updateCollisions();
					rearLeftGroundDeviceBox.updateCollisions();
					rearRightGroundDeviceBox.updateCollisions();
					return motion.y;
				}
			}
		}else if(motion.y > 0){
			if(rotation.x > 0 && (frontLeftGroundDeviceBox.isCollided || frontRightGroundDeviceBox.isCollided)){
				double collisionDepth = Math.max(frontLeftGroundDeviceBox.collisionDepth, frontRightGroundDeviceBox.collisionDepth);
				double motionToNotCollide = collisionDepth/SPEED_FACTOR;
				motion.y += motionToNotCollide;
				frontLeftGroundDeviceBox.updateCollisions();
				frontRightGroundDeviceBox.updateCollisions();
				rearLeftGroundDeviceBox.updateCollisions();
				rearRightGroundDeviceBox.updateCollisions();
				return motion.y;
			}else if(rotation.x < 0 && (rearLeftGroundDeviceBox.isCollided || rearRightGroundDeviceBox.isCollided)){
				double collisionDepth = Math.max(rearLeftGroundDeviceBox.collisionDepth, rearRightGroundDeviceBox.collisionDepth);
				double motionToNotCollide = collisionDepth/SPEED_FACTOR;
				motion.y += motionToNotCollide;
				frontLeftGroundDeviceBox.updateCollisions();
				frontRightGroundDeviceBox.updateCollisions();
				rearLeftGroundDeviceBox.updateCollisions();
				rearRightGroundDeviceBox.updateCollisions();
				return motion.y;
			}
		}
		return 0;
	}*/

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
	/*private double correctPitchMovement(){
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
	}*/

	/**
	 *  Called to adjust the roll of the vehicle to handle collided ground devices.
	 *  Same as correctPitchMovment, just for roll using the X axis rather than Z.
	 */
	/*private double correctRollMovement(){
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
	}*/
	//FIXME remove old code when we get rotations working.

	/**
	 * Method block for ground operations.  This does braking force
	 * and turning for applications independent of vehicle-specific
	 * movement.  Must come AFTER force calculations as it depends on motions.
	 */
	private void performGroundOperations(){
		//Get braking force and apply it to the motions.
		float brakingFactor = getBrakingForceFactor();
		if(brakingFactor > 0){
			double brakingForce = 20F*brakingFactor/currentMass;
			if(brakingForce > Math.abs(velocity)){
				motion.x = 0;
				motion.z = 0;
				rotation.y = 0;
			}else{
				motion.x -= brakingForce*motion.x/Math.abs(velocity);
				motion.z -= brakingForce*motion.z/Math.abs(velocity);
			}
		}
		
		//Add rotation based on our turning factor, and then re-set ground states.
		rotation.y += getTurningFactor();
		normalizedGroundVelocityVector.set(motion.x, 0D, motion.z);
		groundVelocity = normalizedGroundVelocityVector.length()*Math.signum(velocity);
		normalizedGroundVelocityVector.normalize();
		
		//Check how much grip the wheels have.
		float skiddingFactor = getSkiddingFactor();
		if(skiddingFactor != 0 && Math.abs(groundVelocity) > 0.01){
			//Have enough grip, get angle delta between heading and motion.
			Point3d normalizedGroundHeading = new Point3d(headingVector.x, 0, headingVector.z).normalize();
			Point3d crossProduct = normalizedGroundVelocityVector.crossProduct(normalizedGroundHeading);
			double dotProduct = normalizedVelocityVector.dotProduct(headingVector);
			double vectorDelta = Math.toDegrees(Math.atan2(crossProduct.y, dotProduct));
			if(Math.abs(vectorDelta) > 0.001){
				//Check if we are going in reverse and adjust our delta angle if so.
				if(groundVelocity < 0){
					if(vectorDelta >= 90){
						vectorDelta = -(180 - vectorDelta);
					}else if(vectorDelta <= -90){
						vectorDelta = 180 + vectorDelta;
					}
				}

				//Get factor of how much we can correct our turning.
				double motionFactor;
				if(vectorDelta > skiddingFactor){
					motionFactor = skiddingFactor/vectorDelta;
				}else if(vectorDelta < -skiddingFactor){
					motionFactor = -skiddingFactor/vectorDelta;
				}else{
					motionFactor = 1;
				}
				
				//Apply motive changes to the vehicle based on how much we can turn it.
				//We basically take the two components of the motion, and apply one or the other depending on
				//how much delta the vector says we can change.
				Point3d idealMotion = normalizedGroundHeading.multiply(groundVelocity).multiply(motionFactor).add(motion.x*(1-motionFactor), 0D, motion.z*(1-motionFactor));
				motion.x = idealMotion.x;
				motion.z = idealMotion.z;
			}
		}
	}
	
	/**
	 * Returns factor for braking.
	 * Depends on number of grounded core collision sections and braking ground devices.
	 */
	private float getBrakingForceFactor(){
		float brakingFactor = 0;
		//First get the ground device braking contributions.
		//This is both grounded ground devices, and liquid collision boxes that are set as such.
		for(PartGroundDevice groundDevice : groundedGroundDevices){
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
			brakingFactor += 0.5D*groundDeviceBoxes.getBoxesInLiquid();
		}
		
		//Now get any contributions from the colliding collision bits.
		for(BoundingBox box : blockCollisionBoxes){
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
	 * contact with the ground.
	 */
	private float getSkiddingFactor(){
		float skiddingFactor = 0;
		//First check grounded ground devices.
		for(PartGroundDevice groundDevice : groundedGroundDevices){
			skiddingFactor += groundDevice.getLateralFriction() - groundDevice.getFrictionLoss();
		}
		
		//Now check if any collision boxes are in liquid.  Needed for maritime vehicles.
		skiddingFactor += 0.5D*groundDeviceBoxes.getBoxesInLiquid();
		return skiddingFactor > 0 ? skiddingFactor : 0;
	}
	
	/**
	 * Returns factor for turning based on lateral friction, velocity, and wheel distance.
	 * Sign of returned value indicates which direction entity should yaw.
	 * A 0 value indicates no yaw change.
	 */
	private float getTurningFactor(){
		float turningForce = 0;
		float steeringAngle = getSteeringAngle();
		if(steeringAngle != 0){
			float turningFactor = 0;
			float turningDistance = 0;
			//Check grounded ground devices for turn contributions.
			for(PartGroundDevice groundDevice : groundedGroundDevices){
				if(groundDevice.vehicleDefinition.turnsWithSteer){
					turningDistance = (float) Math.max(turningDistance, Math.abs(groundDevice.placementOffset.z));
				}
			}
			if(turningDistance != 0){
				turningFactor = 1.0F;
			}
			
			//Also check for boat propellers, which can make us turn if we are in water.
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
				double steeringFactor = Math.abs(steeringAngle);
				if(turningFactor < 1){
					steeringFactor *= turningFactor;
				}
				//Adjust steering angle to be aligned with distance of the turning part from the center of the vehicle.
				steeringFactor /= turningDistance;
				//Another thing that can affect the steering angle is speed.
				//More speed makes for less wheel turn to prevent crazy circles.
				if(Math.abs(groundVelocity)*SPEED_FACTOR/0.35F - turningFactor/3F > 0){
					steeringFactor *= Math.pow(0.25F, (Math.abs(groundVelocity)*(0.75F + SPEED_FACTOR/0.35F/4F) - turningFactor/3F));
				}
				//Adjust turn force to steer angle based on turning factor.
				turningForce = (float) (steeringFactor*Math.abs(groundVelocity)/2F);
				//Correct for speedFactor changes.
				turningForce *= SPEED_FACTOR/0.35F;
				//Now add the sign to this force based on our steering angle and ground velocity.
				turningForce *= Math.signum(steeringAngle);
				if(Math.abs(groundVelocity) < 0.05){
					turningForce = 0;
				}else if(groundVelocity <= -0.05){
					turningForce = -turningForce;
				}
			}
		}
		return turningForce;
	}
	
	/**
	 * Call this when moving vehicle to ensure they move correctly.
	 * Failure to do this will result in things going badly!
	 */
	private void moveVehicle(){
		//First, update the vehicle ground device boxes.
		groundDeviceBoxes.updateCollisions();
		
		//If any ground devices are collided after our movement, apply corrections to prevent this.
		//The first correction we apply is +y motion.  This counteracts gravity, and ant GDBs that may
		//have been moved into the ground by the application of our motion and rotation.  We do this before collision
		//boxes, as we don't want gravity to cause us to move into something when we really shouldn't move down because
		//all the GDBs prevent this.  In either case, apply +y motion to get all the GDBs out of the ground.
		//This may not be possible, however, if the boxes are too deep into the ground.  We don't want vehicles to
		//instantly climb mountains.  Because of this, we add only 1/4 block, or enough motionY to prevent collision,
		//whichever is the lower of the two.  If we apply boost, update our collision boxes before the next step.
		double groundCollisionBoost = groundDeviceBoxes.getMaxCollisionDepth()/SPEED_FACTOR;
		if(groundCollisionBoost > 0){
			//If adding our boost would make motion.y positive, set our boost to the positive component.
			//This will remove this component from the motion once we move the vehicle, and will prevent bad physics.
			//If we didn't do this, the vehicle would accelerate upwards whenever we corrected ground devices.
			//Having negative motion.y is okay, as this just means we are falling to the ground via gravity.
			if(motion.y + groundCollisionBoost > 0){
				groundCollisionBoost = Math.min(groundCollisionBoost, 0.1D/SPEED_FACTOR);
				motion.y += groundCollisionBoost;
				groundCollisionBoost = motion.y;
			}else{
				motion.y += groundCollisionBoost; 
			}
			groundDeviceBoxes.updateCollisions();
		}
		
		//After checking the ground devices to ensure we aren't shoving ourselves into the ground, we try to move the vehicle.
		//If the vehicle can move without a collision box colliding with something, then we can move to the re-positioning of the vehicle.
		//If we hit something, however, we need to inhibit the movement so we don't do that.
		//This prevents vehicles from phasing through walls even though they are driving on the ground.
		boolean collisionBoxCollided = false;
		tempBoxAngles.setTo(rotation).add(angles);
		for(BoundingBox box : blockCollisionBoxes){
			tempBoxPosition.setTo(box.localCenter).rotateCoarse(tempBoxAngles).add(position).add(motion.x*SPEED_FACTOR, motion.y*SPEED_FACTOR, motion.z*SPEED_FACTOR);
			if(box.updateCollidingBlocks(world, tempBoxPosition.subtract(box.globalCenter))){
				collisionBoxCollided = true;
				break;
			}
		}
		
		//Handle collision box collisions, if we have any.  Otherwise do ground device operations as normal.
		double groundRotationBoost = 0;
		if(collisionBoxCollided){
			correctCollidingMovement();
		}else{
			groundRotationBoost = groundDeviceBoxes.performPitchCorrection(groundCollisionBoost);
			//FIXME enable when we do roll.
			//groundRotationBoost = groundDeviceBoxes.performRollCorrection(groundCollisionBoost + groundRotationBoost);
		}

		//Now that that the movement has been checked, move the vehicle.
		motionApplied.setTo(motion).multiply(SPEED_FACTOR);
		rotationApplied.setTo(rotation);
		if(!world.isClient()){
			if(!motionApplied.isZero() || !rotationApplied.isZero()){
				addToServerDeltas(motionApplied, rotationApplied);
				InterfaceNetwork.sendToClientsTracking(new PacketVehicleServerMovement((EntityVehicleF_Physics) this, motionApplied, rotationApplied), this);
			}
		}else{
			//Make sure the server is sending delta packets before we try to do delta correction.
			if(!serverDeltaM.isZero()){
				//Get the delta difference, and square it.  Then divide it by 25.
				//This gives us a good "bubberbanding correction" formula for deltas.
				//We add this correction motion to the existing motion applied.
				//We need to keep the sign after squaring, however, as that tells us what direction to apply the deltas in.
				clientDeltaMApplied.setTo(serverDeltaM).subtract(clientDeltaM);
				clientDeltaMApplied.x *= Math.abs(clientDeltaMApplied.x);
				clientDeltaMApplied.y *= Math.abs(clientDeltaMApplied.y);
				clientDeltaMApplied.z *= Math.abs(clientDeltaMApplied.z);
				clientDeltaMApplied.multiply(1D/25D);
				motionApplied.add(clientDeltaMApplied);
				
				clientDeltaRApplied.setTo(serverDeltaR).subtract(clientDeltaR);
				clientDeltaRApplied.x *= Math.abs(clientDeltaRApplied.x);
				clientDeltaRApplied.y *= Math.abs(clientDeltaRApplied.y);
				clientDeltaRApplied.z *= Math.abs(clientDeltaRApplied.z);
				clientDeltaRApplied.multiply(1D/25D);
				rotationApplied.add(clientDeltaRApplied);
				
				
				//FIXME do we need this complex logic? I don't think we do, but we may.
				//Check to make sure the delta is non-zero before trying to do complex math to calculate it.
				//Saves a bit of CPU power due to division and multiplication operations, and prevents constant
				//movement due to floating-point errors.
				/*
				if(serverDeltaM.x - clientDeltaM.x != 0){
					motionApplied.x += (serverDeltaM.x - clientDeltaM.x)/25D*Math.abs(serverDeltaM.x - clientDeltaM.x);
				}else{
					motionApplied.x = 0;
				}
				if(serverDeltaM.y - clientDeltaM.y != 0){
					motionApplied.y += (serverDeltaM.y - clientDeltaM.y)/25D*Math.abs(serverDeltaM.y - clientDeltaM.y);
				}else{
					motionApplied.y = 0;
				}
				if(serverDeltaM.z - clientDeltaM.z != 0){
					motionApplied.z += (serverDeltaM.z - clientDeltaM.z)/25D*Math.abs(serverDeltaM.z - clientDeltaM.z);
				}else{
					motionApplied.z = 0;
				}
				
				if(serverDeltaR.x - clientDeltaR.x != 0){
					rotationApplied.x += (serverDeltaR.x - clientDeltaR.x)/25D*Math.abs(serverDeltaR.x - clientDeltaR.x);
				}else{
					rotationApplied.x = 0;
				}
				if(serverDeltaR.y - clientDeltaR.y != 0){
					rotationApplied.y += (serverDeltaR.y - clientDeltaR.y)/25D*Math.abs(serverDeltaR.y - clientDeltaR.y);
				}else{
					rotationApplied.y = 0;
				}
				if(serverDeltaR.z - clientDeltaR.z != 0){
					rotationApplied.z += (serverDeltaR.z - clientDeltaR.z)/25D*Math.abs(serverDeltaR.z - clientDeltaR.z);
				}else{
					rotationApplied.z = 0;
				}*/
				
				//Add actual movement to client deltas.
				clientDeltaM.add(motionApplied);
				clientDeltaR.add(rotationApplied);
			}
		}
		
		//After all movement is calculated, try and move players on hitboxes.
		//Note that we need to interpolate the delta here based on actual movement, so don't use the vehicle motion!
		//Also note we use the entire hitbox set, not just the block hitboxes.
		if(!motionApplied.isZero() || !rotationApplied.isZero()){
			world.moveEntities(collisionBoxes, position, angles, motionApplied, rotationApplied);
		}
		
		//Now add actual position and angles.  Needs to be done before moving entities to tell the
		//system the initial angle and delta angle for movement calculations.
		position.add(motionApplied);
		angles.add(rotationApplied);
		
		//Before we end this tick we need to remove any motions added for ground devices.  These motions are required 
		//only for the updating of the vehicle position due to rotation operations and should not be considered forces.
		//Leaving them in will cause the physics system to think a force was applied, which will make it behave badly!
		//We need to strip away any positive motion.y we gave the vehicle to get it out of the ground if it
		//collided on its ground devices, as well as any motion.y we added when doing rotation adjustments.
		motion.y -= (groundCollisionBoost + groundRotationBoost);
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
		//First check the X-axis.
		if(motion.x != 0){
			for(BoundingBox box : blockCollisionBoxes){
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
			for(BoundingBox box : blockCollisionBoxes){
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
			for(BoundingBox box : blockCollisionBoxes){
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
			for(BoundingBox box : blockCollisionBoxes){
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
			for(BoundingBox box : blockCollisionBoxes){
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
			for(BoundingBox box : blockCollisionBoxes){
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
		//FIXME see if we need this hack any more.
		if(motion.x > 0){
			//motion.x -= 0.002F;
		}
		if(motion.x < 0){
			//motion.x += 0.002F;
		}
		if(motion.z > 0){
			//motion.z -= 0.002F;
		}
		if(motion.z < 0){
			//motion.z += 0.002F;
		}
	}
	
	public void addToServerDeltas(Point3d motion, Point3d rotation){
		serverDeltaM.add(motion);
		serverDeltaR.add(rotation);
	}
	
	/**
	 * Method block for getting the steering angle of this vehicle.
	 */
	protected abstract float getSteeringAngle();
	
	/**
	 * Method block for force and motion calculations.
	 */
	protected abstract void getForcesAndMotions();
	
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
		data.setPoint3d("serverDeltaM", serverDeltaR);
		data.setPoint3d("serverDeltaM", serverDeltaM);
	}
}
