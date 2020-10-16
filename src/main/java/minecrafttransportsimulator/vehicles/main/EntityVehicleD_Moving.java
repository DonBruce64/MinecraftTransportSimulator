package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.baseclasses.VehicleGroundDeviceCollection;
import minecrafttransportsimulator.mcinterface.IWrapperBlock;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketVehicleServerMovement;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleMotorized;

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
	public boolean goingInReverse;
	public double groundVelocity;
	public EntityVehicleF_Physics towedVehicle;
	public EntityVehicleF_Physics towedByVehicle;
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
	private final Point3d normalizedGroundVelocityVector = new Point3d(0, 0, 0);
	private final Point3d normalizedGroundHeadingVector = new Point3d(0, 0, 0);
  	private final VehicleGroundDeviceCollection groundDeviceBoxes;
    	private double driftForce = 0;
  	private double driveTrain = 0;
  	{
  	if (this.definition.motorized.isFrontWheelDrive && this.definition.motorized.isRearWheelDrive){
  		driveTrain = 100;
  	}else if (definition.motorized.isRearWheelDrive){
  		driveTrain = 20;
  	}else if (definition.motorized.isFrontWheelDrive){
  		driveTrain = -20; 
  	}else{
  		driveTrain = 100;
  	}
  	}
	
	/**List of ground devices on the ground.  Populated after each movement to be used in turning/braking calculations.*/
	protected final List<PartGroundDevice> groundedGroundDevices = new ArrayList<PartGroundDevice>();
	
	
	public EntityVehicleD_Moving(IWrapperWorld world, IWrapperNBT data){
		super(world, data);
		this.locked = data.getBoolean("locked");
		this.parkingBrakeOn = data.getBoolean("parkingBrakeOn");
		this.brakeOn = data.getBoolean("brakeOn");
		this.ownerUUID = data.getString("ownerUUID");
		this.serverDeltaM = data.getPoint3d("serverDeltaM");
		this.serverDeltaR = data.getPoint3d("serverDeltaR");
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
		if(!ConfigSystem.configObject.general.noclipVehicles.value || groundDeviceBoxes.isReady()){
			getForcesAndMotions();
			performGroundOperations();
			moveVehicle();
			if(!world.isClient()){
				dampenControlSurfaces();
			}
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

	/**
	 * Method block for ground operations.  This does braking force
	 * and turning for applications independent of vehicle-specific
	 * movement.  Must come AFTER force calculations as it depends on motions.
	 */
	private void performGroundOperations(){
		//Get braking force and apply it to the motions.
		float brakingFactor = getBrakingForce();
		if(brakingFactor > 0){
			double brakingForce = 20F*brakingFactor/currentMass;
			if(brakingForce > velocity){
				motion.x = 0;
				motion.z = 0;
				rotation.y = 0;
			}else{
				motion.x -= brakingForce*motion.x/velocity;
				motion.z -= brakingForce*motion.z/velocity;
			}
		}
		
		//Add rotation based on our turning factor, and then re-set ground states.
		//For turning, we keep turning momentum if the wheels are turned.
		normalizedGroundVelocityVector.set(motion.x, 0D, motion.z);
		groundVelocity = normalizedGroundVelocityVector.length();
		normalizedGroundVelocityVector.normalize();
		normalizedGroundHeadingVector.set(headingVector.x, 0D, headingVector.z).normalize();
		double turningForce = getDriveTrainSkiddingForce() + getTurningForce();
		double dotProduct = normalizedGroundVelocityVector.dotProduct(normalizedGroundHeadingVector);
		if(!goingInReverse && dotProduct < -0.75 && turningForce == 0){
			goingInReverse = true;
		}else if(goingInReverse && dotProduct > 0.75 && turningForce == 0){
			goingInReverse = false;
		}
		if(turningForce != 0){
			rotation.y += goingInReverse ? -turningForce : turningForce;
		}
		//Check how much grip the wheels have.
		float skiddingFactor = getSkiddingForce();
		if(skiddingFactor != 0 && groundVelocity > 0.01){
			//Have enough grip, get angle delta between heading and motion.
			Point3d crossProduct = normalizedGroundVelocityVector.crossProduct(normalizedGroundHeadingVector);
			double vectorDelta = Math.toDegrees(Math.atan2(crossProduct.y, dotProduct));
			//Check if we are backwards and adjust our delta angle if so.
			if(goingInReverse && dotProduct < 0){
				if(vectorDelta >= 90){
					vectorDelta = -(180 - vectorDelta);
				}else if(vectorDelta <= -90){
					vectorDelta = 180 + vectorDelta;
				}
			}
			
			//If we are offset, adjust our angle.
			if(Math.abs(vectorDelta) > 0.001){
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
				Point3d idealMotion = goingInReverse ? normalizedGroundHeadingVector.copy().multiply(-groundVelocity) : normalizedGroundHeadingVector.copy().multiply(groundVelocity);
				idealMotion.multiply(motionFactor).add(motion.x*(1-motionFactor), 0D, motion.z*(1-motionFactor));
				//TODO remove this when we are sure turning is good.
				//System.out.format("Dot:%f Vel:%f GroundVel:%f Turn:%f InRev:%b Client:%s Delta:%f Factor:%f IdelV:%f\n", dotProduct, motion.length(), groundVelocity, turningForce, goingInReverse, world.isClient() ? "1" : "0", vectorDelta, motionFactor, idealMotion.length());
				motion.x = idealMotion.x;
				motion.z = idealMotion.z;
			}
		}
	}
	
	/**
	 * Returns force for braking.
	 * Depends on number of grounded core collision sections and braking ground devices.
	 */
	private float getBrakingForce(){
		float brakingFactor = 0;
		//First get the ground device braking contributions.
		//This is both grounded ground devices, and liquid collision boxes that are set as such.
		for(PartGroundDevice groundDevice : groundedGroundDevices){
			float addedFactor = 0;
			if(brakeOn || parkingBrakeOn){
				addedFactor = groundDevice.getMotiveFriction();
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
				IWrapperBlock groundBlock = world.getWrapperBlock(groundPosition);
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
	 * Returns rotating force for skidding, based on the vehicle drivetrain and other usual MTS calculations
	 */
	private double getDriveTrainSkiddingForce(){
		float skiddingFactor = getSkiddingForce();
		double dotProduct = normalizedGroundVelocityVector.dotProduct(normalizedGroundHeadingVector);
		//Have enough grip, get angle delta between heading and motion.
		Point3d crossProduct = normalizedGroundVelocityVector.crossProduct(normalizedGroundHeadingVector);
		double vectorDelta = Math.toDegrees(Math.atan2(crossProduct.y, dotProduct));
		//Check if we are backwards and adjust our delta angle if so.
		if(goingInReverse && dotProduct < 0){
			if(vectorDelta >= 90){
				vectorDelta = -(180 - vectorDelta);
			}else if(vectorDelta <= -90){
				vectorDelta = 180 + vectorDelta;
			}
		}
		//Get factor of how much we can "correct" our turning.
		driftForce = vectorDelta * (skiddingFactor / driveTrain);
		return  Math.abs(driftForce) > skiddingFactor ? driftForce : 0;
	}
	
	/**
	 * Returns force for skidding based on lateral friction and velocity.
	 * If the value is non-zero, it indicates that yaw changes from ground
	 * device calculations should be applied due to said devices being in
	 * contact with the ground.
	 */
	private float getSkiddingForce(){
		float skiddingFactor = 0;
		//First check grounded ground devices.
		for(PartGroundDevice groundDevice : groundedGroundDevices){
			skiddingFactor += (groundDevice.getLateralFriction() / 2) - groundDevice.getFrictionLoss();
		}
		
		//Now check if any collision boxes are in liquid.  Needed for maritime vehicles.
		skiddingFactor += 0.5D*groundDeviceBoxes.getBoxesInLiquid();
		return skiddingFactor > 0 ? skiddingFactor : 0;
	}
	
	/**
	 * Returns force for turning based on lateral friction, velocity, and wheel distance.
	 * Sign of returned value indicates which direction entity should yaw.
	 * A 0 value indicates no yaw change.
	 */
	private double getTurningForce(){
		float steeringAngle = getSteeringAngle();
		if(steeringAngle != 0){
			double turningDistance = 0;
			//Check grounded ground devices for turn contributions.
			//Their distance from the center of the vehicle defines our turn arc.
			for(PartGroundDevice groundDevice : groundedGroundDevices){
				if(groundDevice.vehicleDefinition.turnsWithSteer){
					turningDistance = Math.max(turningDistance, Math.abs(groundDevice.placementOffset.z));
				}
			}
			
			//If we didn't find any ground devices to make us turn, check propellers in the water.
			if(turningDistance == 0){
				for(APart part : parts){
					if(part instanceof PartPropeller){
						if(part.isInLiquid()){
							turningDistance = Math.max(turningDistance, Math.abs(part.placementOffset.z));
							break;
						}
					}
				}
			}			
			
			//If we are able to turn, calculate the force we create to do so. 
			if(turningDistance > 0){
				//Steering force is initially is the value of the angle, divided by the distance to the wheels.
				//This means tighter turning for shorter-wheelbase vehicles and more input.
				//This is opposite of the torque-based forces for control surfaces.
				double turningForce = steeringAngle/turningDistance;
				//Decrease force by the speed of the vehicle.  If we are going fast, we can't turn as quickly.
				if(groundVelocity > 0.35D){
					turningForce *= Math.pow(0.25F, groundVelocity - 0.35D);
				}
				//Calculate the force the steering produces.  Start with adjusting the steering factor by the ground velocity.
				//This is because the faster we go the quicker we need to turn to keep pace with the vehicle's movement.
				//We need to take speed-factor into account here, as that will make us move different lengths per tick.
				//Finally, we need to reduce this by a constant to get "proper" force..
				return turningForce*groundVelocity*(SPEED_FACTOR/0.35D)/2D;
			}
		}
		return 0;
	}
	
	/**
	 * Call this when moving vehicle to ensure they move correctly.
	 * Failure to do this will result in things going badly!
	 */
	private void moveVehicle(){
		//First, update the vehicle ground device boxes.
		groundDeviceBoxes.updateCollisions();
		
		//If any ground devices are collided after our movement, apply corrections to prevent this.
		//The first correction we apply is +y motion.  This counteracts gravity, and any GDBs that may
		//have been moved into the ground by the application of our motion and rotation.  We do this before collision
		//boxes, as we don't want gravity to cause us to move into something when we really shouldn't move down because
		//all the GDBs prevent this.  In either case, apply +y motion to get all the GDBs out of the ground.
		//This may not be possible, however, if the boxes are too deep into the ground.  We don't want vehicles to
		//instantly climb mountains.  Because of this, we add only 1/8 block, or enough motionY to prevent collision,
		//whichever is the lower of the two.  If we apply boost, update our collision boxes before the next step.
		double groundCollisionBoost = groundDeviceBoxes.getMaxCollisionDepth()/SPEED_FACTOR;
		if(groundCollisionBoost > 0){
			//If adding our boost would make motion.y positive, set our boost to the positive component.
			//This will remove this component from the motion once we move the vehicle, and will prevent bad physics.
			//If we didn't do this, the vehicle would accelerate upwards whenever we corrected ground devices.
			//Having negative motion.y is okay, as this just means we are falling to the ground via gravity.
			if(motion.y + groundCollisionBoost > 0){
				groundCollisionBoost = Math.min(groundCollisionBoost, 0.125D/SPEED_FACTOR);
				motion.y += groundCollisionBoost;
				groundCollisionBoost = motion.y;
			}else{
				motion.y += groundCollisionBoost;
				groundCollisionBoost = 0;
			}
			groundDeviceBoxes.updateCollisions();
		}
		
		//After checking the ground devices to ensure we aren't shoving ourselves into the ground, we try to move the vehicle.
		//If the vehicle can move without a collision box colliding with something, then we can move to the re-positioning of the vehicle.
		//If we hit something, however, we need to inhibit the movement so we don't do that.
		//This prevents vehicles from phasing through walls even though they are driving on the ground.
		//If we are being towed, don't check for collisions, as this can lead to
		boolean collisionBoxCollided = false;
		if(towedByVehicle == null){
			tempBoxAngles.setTo(rotation).add(angles);
			for(BoundingBox box : blockCollisionBoxes){
				tempBoxPosition.setTo(box.localCenter).rotateCoarse(tempBoxAngles).add(position).add(motion.x*SPEED_FACTOR, motion.y*SPEED_FACTOR, motion.z*SPEED_FACTOR);
				if(box.updateCollidingBlocks(world, tempBoxPosition.subtract(box.globalCenter))){
					collisionBoxCollided = true;
					break;
				}
			}
		}
		
		//Handle collision box collisions, if we have any.  Otherwise do ground device operations as normal.
		double groundRotationBoost = 0;
		if(collisionBoxCollided){
			correctCollidingMovement();
		}else{
			groundRotationBoost = groundDeviceBoxes.performPitchCorrection(groundCollisionBoost);
			groundRotationBoost = groundDeviceBoxes.performRollCorrection(groundCollisionBoost + groundRotationBoost);
		}

		//Now that that the movement has been checked, move the vehicle.
		motionApplied.setTo(motion).multiply(SPEED_FACTOR);
		rotationApplied.setTo(rotation);
		if(!world.isClient()){
			if(!motionApplied.isZero() || !rotationApplied.isZero()){
				addToServerDeltas(motionApplied, rotationApplied);
				MasterLoader.networkInterface.sendToAllClients(new PacketVehicleServerMovement((EntityVehicleF_Physics) this, motionApplied, rotationApplied));
			}
		}else{
			//Make sure the server is sending delta packets before we try to do delta correction.
			if(!serverDeltaM.isZero()){
				//Get the delta difference, and square it.  Then divide it by 25.
				//This gives us a good "rubberbanding correction" formula for deltas.
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
				//TODO figure out why angles changed after spawning.
				//if(world.isClient())System.out.format("Angle:%f Applied:%f Server:%f Client:%f\n", angles.x, clientDeltaRApplied.x, serverDeltaR.x, clientDeltaR.x);
				rotationApplied.add(clientDeltaRApplied);
				
				//Add actual movement to client deltas to prevent further corrections.
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
		
		//Now add actual position and angles.
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
	public void save(IWrapperNBT data){
		super.save(data);
		data.setBoolean("locked", locked);
		data.setBoolean("brakeOn", brakeOn);
		data.setBoolean("parkingBrakeOn", parkingBrakeOn);
		data.setString("ownerUUID", ownerUUID);
		data.setPoint3d("serverDeltaM", serverDeltaM);
		data.setPoint3d("serverDeltaR", serverDeltaR);
	}
}
