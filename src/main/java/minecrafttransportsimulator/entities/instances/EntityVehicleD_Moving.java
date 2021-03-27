package minecrafttransportsimulator.entities.instances;

import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.VehicleGroundDeviceCollection;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.tileentities.components.RoadFollowingState;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane.LaneSelectionRequest;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.jsondefs.JSONConnection;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketVehicleServerMovement;
import minecrafttransportsimulator.packets.instances.PacketVehicleTrailerChange;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.systems.ConfigSystem;

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
	public static final byte MAX_BRAKE = 100;
	public byte brake;
	public boolean parkingBrakeOn;
	
	//Internal states.
	public boolean goingInReverse;
	public boolean slipping;
	public boolean skidSteerActive;
	public double groundVelocity;
	
	//Towing data.
	public EntityVehicleF_Physics towedVehicle;
	public EntityVehicleF_Physics towedByVehicle;
	public JSONConnection activeHitchConnection;
	public JSONConnection activeHookupConnection;
	public APart activeHitchPart;
	public APart activeHookupPart;
	private String towedVehicleSavedID;
	private String towedByVehicleSavedID;
	private int activeHitchConnectionSavedIndex;
	private Point3d activeHitchPartSavedOffset;
	private int activeHookupConnectionSavedIndex;
	private Point3d activeHookupPartSavedOffset;
	
	//Road-following data.
	protected RoadFollowingState frontFollower;
	protected RoadFollowingState rearFollower;
	protected LaneSelectionRequest selectedSegment = LaneSelectionRequest.NONE;
	
	//Internal movement variables.
	private final Point3d serverDeltaM;
	private final Point3d serverDeltaR;
	private final Point3d clientDeltaM;
	private final Point3d clientDeltaR;
	private final Point3d clientDeltaMApplied = new Point3d();
	private final Point3d clientDeltaRApplied = new Point3d();
	private final Point3d roadMotion = new Point3d();
	private final Point3d roadRotation = new Point3d();
	private final Point3d motionApplied = new Point3d();
	private final Point3d rotationApplied = new Point3d();
	private final Point3d tempBoxPosition = new Point3d();
	private final Point3d tempBoxRotation = new Point3d();
	private final Point3d normalizedGroundVelocityVector = new Point3d();
	private final Point3d normalizedGroundHeadingVector = new Point3d();
  	public final VehicleGroundDeviceCollection groundDeviceCollective;
	
	public EntityVehicleD_Moving(WrapperWorld world, WrapperNBT data){
		super(world, data);
		this.parkingBrakeOn = data.getBoolean("parkingBrakeOn");
		this.brake = (byte) data.getInteger("brake");
		
		this.towedVehicleSavedID = data.getString("towedVehicleSavedID");
		this.towedByVehicleSavedID = data.getString("towedByVehicleSavedID");
		this.activeHitchConnectionSavedIndex = data.getInteger("activeHitchConnectionSavedIndex");
		this.activeHookupConnectionSavedIndex = data.getInteger("activeHookupConnectionSavedIndex");
		this.activeHitchPartSavedOffset = data.getPoint3d("activeHitchPartSavedOffset");
		this.activeHookupPartSavedOffset = data.getPoint3d("activeHookupPartSavedOffset");
		
		this.serverDeltaM = data.getPoint3d("serverDeltaM");
		this.serverDeltaR = data.getPoint3d("serverDeltaR");
		this.clientDeltaM = serverDeltaM.copy();
		this.clientDeltaR = serverDeltaR.copy();
		this.groundDeviceCollective = new VehicleGroundDeviceCollection((EntityVehicleF_Physics) this);
	}
	
	@Override
	public void update(){
		//Before calling super, see if we need to link a towed or towed by vehicle.
		//We need to wait on this in case the vehicle didn't load at the same time.
		if(!towedVehicleSavedID.isEmpty() || !towedByVehicleSavedID.isEmpty()){
			try{
				if(!towedVehicleSavedID.isEmpty()){
					towedVehicle = AEntityA_Base.getEntity(world, towedVehicleSavedID);
					if(towedVehicle != null){
						if(!activeHitchPartSavedOffset.isZero()){
							activeHitchPart = getPartAtLocation(activeHitchPartSavedOffset);
							activeHitchConnection = activeHitchPart.definition.connections.get(activeHitchConnectionSavedIndex);
						}else{
							activeHitchConnection = definition.connections.get(activeHitchConnectionSavedIndex);
						}
						towedVehicleSavedID = "";
					}
				}
				if(!towedByVehicleSavedID.isEmpty()){
					towedByVehicle = AEntityA_Base.getEntity(world, towedByVehicleSavedID);
					if(!activeHookupPartSavedOffset.isZero()){
						activeHookupPart = getPartAtLocation(activeHookupPartSavedOffset);
						activeHookupConnection = activeHookupPart.definition.connections.get(activeHookupConnectionSavedIndex);
					}else{
						activeHookupConnection = definition.connections.get(activeHookupConnectionSavedIndex);
					}
					towedByVehicleSavedID = "";
				}
			}catch(Exception e){
				InterfaceCore.logError("Could not connect trailer to vehicle.  Did the JSON change?");
				towedVehicle = null;
				activeHitchConnection = null;
				towedByVehicle = null;
				activeHookupConnection = null;
				towedVehicleSavedID = "";
				towedByVehicleSavedID = "";
			}
		}
		super.update();
		
		//Update our GDB members if any of our ground devices don't have the same total offset as placement.
		//This is required to move the GDBs if the GDs move.
		for(APart part : parts){
			if(part instanceof PartGroundDevice){
				if(!part.localOffset.equals(part.prevLocalOffset)){
					groundDeviceCollective.updateBounds();
					break;
				}
			}
		}
		
		//Now do update calculations and logic.
		if(!ConfigSystem.configObject.general.noclipVehicles.value || groundDeviceCollective.isReady()){
			getForcesAndMotions();
			performGroundOperations();
			moveVehicle();
			if(!world.isClient()){
				dampenControlSurfaces();
			}
		}
		
		//Update parts after all movement is done.
		updateParts();
	}
	
	@Override
	public void addPart(APart part, boolean sendPacket){
		super.addPart(part, sendPacket);
		groundDeviceCollective.updateMembers();
		groundDeviceCollective.updateBounds();
	}
	
	@Override
	public void removePart(APart part, Iterator<APart> iterator){
		super.removePart(part, iterator);
		groundDeviceCollective.updateMembers();
		groundDeviceCollective.updateBounds();
	}
	
	@Override
	public boolean needsChunkloading(){
		return rearFollower != null || (towedByVehicle != null && towedByVehicle.rearFollower != null);
	}
	
	/**
	 * Returns the follower for the rear of the vehicle.  Front follower should
	 * be obtained by getting the point from this follower the distance away from the
	 * front and the rear position.  This may be the same curve, this may not.
	 */
	private RoadFollowingState getFollower(){
		Point3d contactPoint = groundDeviceCollective.getContactPoint(false);
		if(contactPoint != null){
			contactPoint.rotateCoarse(angles).add(position);
			Point3d testPoint = new Point3d();
			ABlockBase block =  world.getBlock(contactPoint);
			if(block instanceof BlockCollision){
				TileEntityRoad road = ((BlockCollision) block).getMasterRoad(world, contactPoint);
				if(road != null){
					//Check to see which lane we are on, if any.
					for(RoadLane lane : road.lanes){
						//Check path-points on the curve.  If our angles and position are close, set this as the curve.
						for(BezierCurve curve : lane.curves){
							for(float f=0; f<curve.pathLength; ++f){
								curve.setPointToPositionAt(testPoint, f);
								testPoint.add(road.position.x, road.position.y, road.position.z);
								if(testPoint.distanceTo(contactPoint) < 1){
									curve.setPointToRotationAt(testPoint, f);
									boolean sameDirection = Math.abs(testPoint.getClampedYDelta(angles.y)) < 10;
									boolean oppositeDirection = Math.abs(testPoint.getClampedYDelta(angles.y)) > 170;
									if(sameDirection || oppositeDirection){
										return new RoadFollowingState(lane, curve, sameDirection, f);
									}
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Method block for ground operations.  This does braking force
	 * and turning for applications independent of vehicle-specific
	 * movement.  Must come AFTER force calculations as it depends on motions.
	 */
	private void performGroundOperations(){
		//Get braking force and apply it to the motions.
		float brakingFactor = getBrakingForce()*definition.motorized.brakingFactor;
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
		double turningForce = getTurningForce();
		double dotProduct = normalizedGroundVelocityVector.dotProduct(normalizedGroundHeadingVector);
		//TODO having velocity in the formula here has the potential to lead to hang-ups. Use packets perhaps?
		if(skidSteerActive){
			goingInReverse = false;
		}else{
			if(!goingInReverse && dotProduct < -0.75 && (turningForce == 0 || velocity < 0.1)){
				goingInReverse = true;
			}else if(goingInReverse && dotProduct > 0.75 && (turningForce == 0 || velocity < 0.1)){
				goingInReverse = false;
			}
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
				motion.x = idealMotion.x;
				motion.z = idealMotion.z;
				
				//If we are slipping while turning, spawn block particles.
				//Only do this as a main vehicle.  If we are a trailer, we don't do this unless the vehicle towing us is.
				if(towedByVehicle == null ? (world.isClient() && motionFactor != 1 && velocity > 0.75) : towedByVehicle.slipping){
					slipping = true;
					for(byte i=0; i<4; ++i){
						groundDeviceCollective.spawnSlippingParticles();
					}
				}else{
					slipping = false;
				}
			}
		}
	}
	
	/**
	 * Returns force for braking.
	 * Depends on number of grounded core collision sections and braking ground devices.
	 */
	private float getBrakingForce(){
		float brakingPower = parkingBrakeOn ? 1.0F : brake/(float)MAX_BRAKE;
		float brakingFactor = 0;
		//First get the ground device braking contributions.
		//This is both grounded ground devices, and liquid collision boxes that are set as such.
		if(brakingPower > 0){
			for(PartGroundDevice groundDevice : groundDeviceCollective.groundedGroundDevices){
				float groundDevicePower = groundDevice.getMotiveFriction();
				if(groundDevicePower != 0){
					brakingFactor += Math.max(groundDevicePower - groundDevice.getFrictionLoss(), 0);
				}
			}
			brakingFactor += 0.5D*groundDeviceCollective.getNumberBoxesInLiquid();
		}
		
		//Get any contributions from the colliding collision bits.
		for(BoundingBox box : allBlockCollisionBoxes){
			if(!box.collidingBlockPositions.isEmpty()){
				if(!world.isAir(box.globalCenter)){
					float frictionLoss = 0.6F - world.getBlockSlipperiness(box.globalCenter) + world.getRainStrength(box.globalCenter)*0.1F;
					brakingFactor += Math.max(2.0 - frictionLoss, 0);
				}
			}
		}
		
		//Get any contributions from liquid boxes that aren't in liquids.
		brakingFactor += 2.0F*groundDeviceCollective.getNumberCollidedLiquidBoxes();
		
		return brakingFactor;
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
		for(PartGroundDevice groundDevice : groundDeviceCollective.groundedGroundDevices){
			skiddingFactor += groundDevice.getLateralFriction() - groundDevice.getFrictionLoss();
		}
		
		//Now check if any collision boxes are in liquid.  Needed for maritime vehicles.
		skiddingFactor += 0.5D*groundDeviceCollective.getNumberBoxesInLiquid();
		return skiddingFactor > 0 ? skiddingFactor : 0;
	}
	
	/**
	 * Returns force for turning based on lateral friction, velocity, and wheel distance.
	 * Sign of returned value indicates which direction entity should yaw.
	 * A 0 value indicates no yaw change.
	 */
	private double getTurningForce(){
		float steeringAngle = getSteeringAngle()*45;
		skidSteerActive = false;
		if(steeringAngle != 0){
			double turningDistance = 0;
			//Check grounded ground devices for turn contributions.
			//Their distance from the center of the vehicle defines our turn arc.
			//Don't use fake ground devices here as it'll mess up math for vehicles.
			boolean treadsOnly = true;
			for(PartGroundDevice groundDevice : groundDeviceCollective.groundedGroundDevices){
				if(groundDevice.placementDefinition.turnsWithSteer && !groundDevice.isFake()){
					turningDistance = Math.max(turningDistance, Math.abs(groundDevice.placementOffset.z));
					if(treadsOnly && !groundDevice.definition.ground.isTread){
						treadsOnly = false;
					}
				}
			}
			
			//If we only have treads, double the distance.  This accounts for tracked-only vehicles.
			if(treadsOnly){
				turningDistance *= 2;
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
				//If we are vehicle that can do skid-steer, and that's active, do so now.
				if(definition.motorized.hasSkidSteer){
					if(groundDeviceCollective.isReady() && groundVelocity < 0.05){
						boolean foundNeutralEngine = false;
						boolean leftWheelGrounded = false;
						boolean rightWheelGrounded = false;
						for(APart part : parts){
							if(part instanceof PartGroundDevice){
								if(groundDeviceCollective.groundedGroundDevices.contains(part)){
									if(part.placementOffset.x > 0){
										leftWheelGrounded = true;
									}else{
										rightWheelGrounded = true;
									}
								}
							}else if(part instanceof PartEngine){
								if(((PartEngine) part).currentGear == 0 && ((PartEngine) part).state.running){
									foundNeutralEngine = true;
								}
							}
						}
						skidSteerActive = foundNeutralEngine && leftWheelGrounded && rightWheelGrounded;
					}
					
					//If skidSteer is active, do it now.
					if(skidSteerActive){
						return steeringAngle/20D;
					}
				}
				
				//Steering force is initially is the value of the angle, divided by the distance to the wheels.
				//This means tighter turning for shorter-wheelbase vehicles and more input.
				//This is opposite of the torque-based forces for control surfaces.
				double turningForce = steeringAngle/turningDistance;
				//Decrease force by the speed of the vehicle.  If we are going fast, we can't turn as quickly.
				if(groundVelocity > 0.35D){
					turningForce *= Math.pow(0.25F, (groundVelocity - 0.35D)) + (groundVelocity * (definition.motorized.downForce / 100));
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
		groundDeviceCollective.updateCollisions();
		
		//If we aren't on a road, try to find one.
		//Only do this if we aren't turning, and if we aren't being towed, and we aren't an aircraft.
		if(towedByVehicle != null || definition.motorized.isAircraft){
			frontFollower = null;
			rearFollower = null;
		}else if((frontFollower == null || rearFollower == null) && ticksExisted%20 == 0){
			rearFollower = getFollower();
			if(rearFollower != null){
				double pointDelta = groundDeviceCollective.getContactPoint(false).distanceTo(groundDeviceCollective.getContactPoint(true));
				frontFollower = new RoadFollowingState(rearFollower.lane, rearFollower.curve, rearFollower.goingForwards, rearFollower.currentSegment).updateCurvePoints((float) pointDelta, LaneSelectionRequest.NONE);
			}
		}
		
		//If we are on a road, we need to bypass the logic for pitch/yaw/roll checks, and GDB checks.
		//This is because if we are on a road we need to follow the road's curve.
		//If we have both followers, do road-following logic.
		//If we don't, or we're turning off the road, do normal vehicle logic.
		roadMotion.set(0, 0, 0);
		roadRotation.set(0, 0, 0);
		if(frontFollower != null && rearFollower != null){
			//Check for the potential to change the requested segment.
			//We can only do this if both our followers are on the same semgnt.
			LaneSelectionRequest requestedSegment;
			if(!(variablesOn.contains(LightType.LEFTTURNLIGHT.lowercaseName) ^ variablesOn.contains(LightType.RIGHTTURNLIGHT.lowercaseName))){
				requestedSegment = LaneSelectionRequest.NONE;
			}else if(variablesOn.contains(LightType.LEFTTURNLIGHT.lowercaseName)){
				requestedSegment = goingInReverse ? LaneSelectionRequest.RIGHT : LaneSelectionRequest.LEFT;
			}else{
				requestedSegment = goingInReverse ? LaneSelectionRequest.LEFT : LaneSelectionRequest.RIGHT;
			}
			if(frontFollower.equals(rearFollower)){
				selectedSegment = requestedSegment;
			}
			
			float segmentDelta = (float) (goingInReverse ? -velocity*SPEED_FACTOR : velocity*SPEED_FACTOR);
			frontFollower = frontFollower.updateCurvePoints(segmentDelta, selectedSegment);
			rearFollower = rearFollower.updateCurvePoints(segmentDelta, selectedSegment);
			Point3d rearPoint = groundDeviceCollective.getContactPoint(false);
			
			//Check to make sure followers are still valid, and do logic.
			if(frontFollower != null && rearFollower != null && rearPoint != null){
				//Set our position so we're aligned with the road.
				//To do this, we get the distance between our contact points for front and rear, and then interpolate between them.
				//First get the rear point.  This defines the delta for the movement of the vehicle.
				rearPoint.rotateFine(angles).add(position);
				Point3d rearDesiredPoint = rearFollower.getCurrentPoint();
				
				//Apply the motion based on the delta between the actual and desired.
				//Also set motion Y to 0 in case we were doing ground device things.
				roadMotion.setTo(rearDesiredPoint).subtract(rearPoint);
				if(roadMotion.length() > 1){
					roadMotion.set(0, 0, 0);
					frontFollower = null;
					rearFollower = null;
				}else{
					motion.y = 0;
					
					//Now get the front desired point.  We don't care about actual point here, as we set angle base on the point delta.
					//Desired angle is the one that gives us the vector between the front and rear points.
					Point3d desiredVector = frontFollower.getCurrentPoint().subtract(rearDesiredPoint);
					double yawDelta = Math.toDegrees(Math.atan2(desiredVector.x, desiredVector.z));
					double pitchDelta = -Math.toDegrees(Math.atan2(desiredVector.y, Math.hypot(desiredVector.x, desiredVector.z)));
					double rollDelta = 0;
					roadRotation.set(pitchDelta - angles.x, yawDelta, rollDelta - angles.z);
					roadRotation.y = roadRotation.getClampedYDelta(angles.y);
					if(!world.isClient()){
						addToSteeringAngle((float) (goingInReverse ? -roadRotation.y : roadRotation.y));
					}
				}
			}else{
				//Set followers to null, as something is invalid.
				//InterfaceChunkloader.removeEntityTicket(this);
				frontFollower = null;
				rearFollower = null;
			}
		}
		
		double groundCollisionBoost = 0;
		double groundRotationBoost = 0;
		//If followers aren't valid, do normal logic.
		if(frontFollower == null || rearFollower == null){
			//If any ground devices are collided after our movement, apply corrections to prevent this.
			//The first correction we apply is +y motion.  This counteracts gravity, and any GDBs that may
			//have been moved into the ground by the application of our motion and rotation.  We do this before collision
			//boxes, as we don't want gravity to cause us to move into something when we really shouldn't move down because
			//all the GDBs prevent this.  In either case, apply +y motion to get all the GDBs out of the ground.
			//This may not be possible, however, if the boxes are too deep into the ground.  We don't want vehicles to
			//instantly climb mountains.  Because of this, we add only 1/8 block, or enough motionY to prevent collision,
			//whichever is the lower of the two.  If we apply boost, update our collision boxes before the next step.
			//Note that this logic is not applied on trailers, as they use special checks with only rotations for movement.
			if(towedByVehicle == null){
				groundCollisionBoost = groundDeviceCollective.getMaxCollisionDepth()/SPEED_FACTOR;
				if(groundCollisionBoost > 0){
					//If adding our boost would make motion.y positive, set our boost to the positive component.
					//This will remove this component from the motion once we move the vehicle, and will prevent bad physics.
					//If we didn't do this, the vehicle would accelerate upwards whenever we corrected ground devices.
					//Having negative motion.y is okay, as this just means we are falling to the ground via gravity.
					if(motion.y + groundCollisionBoost > 0){
						groundCollisionBoost = Math.min(groundCollisionBoost, ConfigSystem.configObject.general.climbSpeed.value/SPEED_FACTOR);
						motion.y += groundCollisionBoost;
						groundCollisionBoost = motion.y;
					}else{
						motion.y += groundCollisionBoost;
						groundCollisionBoost = 0;
					}
					groundDeviceCollective.updateCollisions();
				}
			}
			
			//After checking the ground devices to ensure we aren't shoving ourselves into the ground, we try to move the vehicle.
			//If the vehicle can move without a collision box colliding with something, then we can move to the re-positioning of the vehicle.
			//If we hit something, however, we need to inhibit the movement so we don't do that.
			//This prevents vehicles from phasing through walls even though they are driving on the ground.
			//If we are being towed, apply this movement to the towing vehicle, not ourselves, as this can lead to the vehicle getting stuck.
			//If the collision box is a liquid box, don't use it, as that gets used in ground device calculations instead.
			if(isCollisionBoxCollided()){
				if(towedByVehicle != null){
					Point3d initalMotion = motion.copy();
					if(correctCollidingMovement()){
						return;
					}
					towedByVehicle.motion.add(motion).subtract(initalMotion);
				}else if(correctCollidingMovement()){
					return;
				}
				
			}else if(towedByVehicle == null || (towedByVehicle.activeHitchConnection != null && !towedByVehicle.activeHitchConnection.mounted)){
				groundRotationBoost = groundDeviceCollective.performPitchCorrection(groundCollisionBoost);
				//Don't do roll correction if we don't have roll.
				if(groundDeviceCollective.canDoRollChecks()){
					groundRotationBoost = groundDeviceCollective.performRollCorrection(groundCollisionBoost + groundRotationBoost);
				}
				
				//If we are flagged as a tilting vehicle try to keep us upright, unless we are turning, in which case turn into the turn.
				if(definition.motorized.maxTiltAngle != 0){
					rotation.z = -angles.z - definition.motorized.maxTiltAngle*2.0*Math.min(0.5, velocity/2D)*getSteeringAngle();
					if(Double.isNaN(rotation.z)){
						rotation.z = 0;
					}
				}
			}
		}

		//Now that that the movement has been checked, move the vehicle.
		motionApplied.setTo(motion).multiply(SPEED_FACTOR).add(roadMotion);
		rotationApplied.setTo(rotation).add(roadRotation);
		if(!world.isClient()){
			if(!motionApplied.isZero() || !rotationApplied.isZero()){
				addToServerDeltas(motionApplied, rotationApplied);
				InterfacePacket.sendToAllClients(new PacketVehicleServerMovement((EntityVehicleF_Physics) this, motionApplied, rotationApplied));
			}
		}else{
			//Make sure the server is sending delta packets before we try to do delta correction.
			if(!serverDeltaM.isZero() || !serverDeltaR.isZero()){
				//Get the delta difference, and square it.  Then divide it by 25.
				//This gives us a good "rubberbanding correction" formula for deltas.
				//We add this correction motion to the existing motion applied.
				//We need to keep the sign after squaring, however, as that tells us what direction to apply the deltas in.
				clientDeltaMApplied.setTo(serverDeltaM).subtract(clientDeltaM);
				clientDeltaMApplied.x *= Math.abs(clientDeltaMApplied.x);
				clientDeltaMApplied.y *= Math.abs(clientDeltaMApplied.y);
				clientDeltaMApplied.z *= Math.abs(clientDeltaMApplied.z);
				clientDeltaMApplied.multiply(1D/25D);
				if(clientDeltaMApplied.x > 5){
					clientDeltaMApplied.x = 5;
				}
				if(clientDeltaMApplied.y > 5){
					clientDeltaMApplied.y = 5;
				}
				if(clientDeltaMApplied.z > 5){
					clientDeltaMApplied.z = 5;
				}
				motionApplied.add(clientDeltaMApplied);
				
				clientDeltaRApplied.setTo(serverDeltaR).subtract(clientDeltaR);
				clientDeltaRApplied.x *= Math.abs(clientDeltaRApplied.x);
				clientDeltaRApplied.y *= Math.abs(clientDeltaRApplied.y);
				clientDeltaRApplied.z *= Math.abs(clientDeltaRApplied.z);
				clientDeltaRApplied.multiply(1D/25D);
				//Only apply delta y if it's less than 5.
				//If they're higher, we could have bad packets or a re-do of rotations.
				rotationApplied.add(clientDeltaRApplied);
				if(rotationApplied.y > 5){
					rotationApplied.y = 5;
				}else if(rotationApplied.y < -5){
					rotationApplied.y = -5;
				}
				
				//Add actual movement to client deltas to prevent further corrections.
				clientDeltaM.add(motionApplied);
				clientDeltaR.add(rotationApplied);
			}
		}
		
		//After all movement is calculated, try and move players on hitboxes.
		//Note that we need to interpolate the delta here based on actual movement, so don't use the vehicle motion!
		//Also note we use the entire hitbox set, not just the block hitboxes.
		if(!motionApplied.isZero() || !rotationApplied.isZero()){
			world.moveEntities(allCollisionBoxes, position, angles, motionApplied, rotationApplied);
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
	 *  Checks if we have a collided collision box.  If so, true is returned.
	 */
	private boolean isCollisionBoxCollided(){
		tempBoxRotation.setTo(rotation);
		for(BoundingBox box : allBlockCollisionBoxes){
			tempBoxPosition.setTo(box.globalCenter).subtract(position).rotateCoarse(tempBoxRotation).add(position).add(motion.x*SPEED_FACTOR, motion.y*SPEED_FACTOR, motion.z*SPEED_FACTOR);
			if(!box.collidesWithLiquids && box.updateCollidingBlocks(world, tempBoxPosition.subtract(box.globalCenter))){
				return true;
			}
		}
		return false;
	}
	
	/**
	 *  If a collision box collided, we need to restrict our proposed movement.
	 *  Do this by removing motions that cause collisions.
	 *  If the motion has a value of 0, skip it as it couldn't have caused the collision.
	 *  Note that even though motionY may have been adjusted for ground device operation prior to this call,
	 *  we shouldn't have an issue with the change as this logic takes priority over that logic to ensure 
	 *  no collision box collides with another block, even if it requires all the ground devices to be collided.
	 *  If true is returned here, it means this vehicle was destroyed in a collision, and no further processing should
	 *  be done on it as states may be un-defined.
	 */
	private boolean correctCollidingMovement(){
		//First check the X-axis.
		if(motion.x != 0){
			for(BoundingBox box : allBlockCollisionBoxes){
				double collisionDepth = getCollisionForAxis(box, true, false, false);
				if(collisionDepth == -1){
					return true;
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
			for(BoundingBox box : allBlockCollisionBoxes){
				double collisionDepth = getCollisionForAxis(box, false, false, true);
				if(collisionDepth == -1){
					return true;
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
			for(BoundingBox box : allBlockCollisionBoxes){
				double collisionDepth = getCollisionForAxis(box, false, true, false);
				if(collisionDepth == -1){
					return true;
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
			tempBoxRotation.set(0D, rotation.y, 0D);
			for(BoundingBox box : allBlockCollisionBoxes){
				while(rotation.y != 0){
					tempBoxPosition.setTo(box.globalCenter).subtract(position).rotateCoarse(tempBoxRotation).add(position).add(motion.x*SPEED_FACTOR, motion.y*SPEED_FACTOR, motion.z*SPEED_FACTOR);
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
			tempBoxRotation.set(rotation.x, rotation.y, 0D);
			for(BoundingBox box : allBlockCollisionBoxes){
				while(rotation.x != 0){
					tempBoxPosition.setTo(box.globalCenter).subtract(position).rotateCoarse(tempBoxRotation).add(position).add(motion.x*SPEED_FACTOR, motion.y*SPEED_FACTOR, motion.z*SPEED_FACTOR);
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
			tempBoxRotation.setTo(rotation);
			for(BoundingBox box : allBlockCollisionBoxes){
				while(rotation.z != 0){
					tempBoxPosition.setTo(box.globalCenter).subtract(position).rotateCoarse(tempBoxRotation).add(position).add(motion.x*SPEED_FACTOR, motion.y*SPEED_FACTOR, motion.z*SPEED_FACTOR);
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
		return false;
	}
	
	/**
	 * Returns true if this vehicle, or any of its parts, have hitches on them.
	 */
	public boolean hasHitch(){
		if(definition.connections != null){
			for(JSONConnection connection : definition.connections){
				if(!connection.hookup){
					return true;
				}
			}
		}
		for(APart part : parts){
			if(part.definition.connections != null){
				for(JSONConnection connection : part.definition.connections){
					if(!connection.hookup){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Returns the current hitch offset for this vehicle.
	 * If this vehicle doesn't have a current active hitch, null is returned.
	 */
	public Point3d getHitchOffset(){
		if(activeHitchConnection != null){
			if(activeHitchPart != null){
				return activeHitchConnection.pos.copy().rotateFine(activeHitchPart.localAngles).add(activeHitchPart.localOffset); 
			}else{
				return activeHitchConnection.pos;
			}
		}else{
			return null;
		}
	}
	
	/**
	 * Returns the current hookup offset for this vehicle.
	 * If this vehicle doesn't have a current active hookup, null is returned.
	 */
	public Point3d getHookupOffset(){
		if(activeHookupConnection != null){
			if(activeHookupPart != null){
				return activeHookupConnection.pos.copy().rotateFine(activeHookupPart.localAngles).add(activeHookupPart.localOffset); 
			}else{
				return activeHookupConnection.pos;
			}
		}else{
			return null;
		}
	}
	
	/**
	 * Tries to connect the passed-in vehicle to this vehicle.
	 */
	public TrailerConnectionResult tryToConnect(EntityVehicleD_Moving trailer){
		//Init variables.
		boolean matchingConnection = false;
		boolean trailerInRange = false;
		
		//Make sure we have hitches to check before doing this logic.
		if(hasHitch()){
			//First make sure the vehicle is in-range.  This is done by checking if the vehicle is even remotely close enough.
			double trailerDistance = position.distanceTo(trailer.position);
			if(trailerDistance < 25){
				//Check all connections.
				
				//First check vehicle-vehicle connections.
				switch(tryToConnectConnections(definition.connections, trailer.definition.connections, this, trailer, null, null)){
					case TRAILER_CONNECTED : return TrailerConnectionResult.TRAILER_CONNECTED;
					case TRAILER_TOO_FAR : matchingConnection = true; break;
					case TRAILER_WRONG_HITCH : trailerInRange = true; break;
					case NO_TRAILER_NEARBY : break;
				}
				
				//Check part-vehicle and part-part connections.
				for(APart vehiclePart : parts){
					//Part-vehicle
					switch(tryToConnectConnections(vehiclePart.definition.connections, trailer.definition.connections, this, trailer, vehiclePart, null)){
						case TRAILER_CONNECTED : return TrailerConnectionResult.TRAILER_CONNECTED;
						case TRAILER_TOO_FAR : matchingConnection = true; break;
						case TRAILER_WRONG_HITCH : trailerInRange = true; break;
						case NO_TRAILER_NEARBY : break;
					}
					
					//Part-part;
					for(APart trailerPart : trailer.parts){
						switch(tryToConnectConnections(vehiclePart.definition.connections, trailerPart.definition.connections, this, trailer, vehiclePart, trailerPart)){
							case TRAILER_CONNECTED : return TrailerConnectionResult.TRAILER_CONNECTED;
							case TRAILER_TOO_FAR : matchingConnection = true; break;
							case TRAILER_WRONG_HITCH : trailerInRange = true; break;
							case NO_TRAILER_NEARBY : break;
						}
					}
				}
				
				//Check vehicle-part connections.
				for(APart trailerPart : trailer.parts){
					switch(tryToConnectConnections(definition.connections, trailerPart.definition.connections, this, trailer, null, trailerPart)){
						case TRAILER_CONNECTED : return TrailerConnectionResult.TRAILER_CONNECTED;
						case TRAILER_TOO_FAR : matchingConnection = true; break;
						case TRAILER_WRONG_HITCH : trailerInRange = true; break;
						case NO_TRAILER_NEARBY : break;
					}
				}
			}
		}
		
		//Return results.
		if(matchingConnection && !trailerInRange){
			return TrailerConnectionResult.TRAILER_TOO_FAR;
		}else if(!matchingConnection && trailerInRange){
			return TrailerConnectionResult.TRAILER_WRONG_HITCH;
		}else{
			return TrailerConnectionResult.NO_TRAILER_NEARBY;
		}
	}
	
	/**
	 * Helper block for checking if two connection sets can connect.
	 */
	private static TrailerConnectionResult tryToConnectConnections(List<JSONConnection> firstConnections, List<JSONConnection> secondConnections, EntityVehicleD_Moving firstVehicle, EntityVehicleD_Moving secondVehicle, APart optionalFirstPart, APart optionalSecondPart){
		//Check to make sure wer're being fed actual connections.
		if(firstConnections != null && secondConnections != null){
			//Create status variables.
			boolean matchingConnection = false;
			boolean trailerInRange = false;
			for(JSONConnection firstConnection : firstConnections){
				for(JSONConnection secondConnection : secondConnections){
					if(!firstConnection.hookup && secondConnection.hookup){
						Point3d hitchPos = firstConnection.pos.copy();
						if(optionalFirstPart != null){
							hitchPos.rotateCoarse(optionalFirstPart.localAngles).add(optionalFirstPart.localOffset);
						}
						hitchPos.rotateCoarse(firstVehicle.angles).add(firstVehicle.position);
						Point3d hookupPos = secondConnection.pos.copy();
						if(optionalSecondPart != null){
							hookupPos.rotateCoarse(optionalSecondPart.localAngles).add(optionalSecondPart.localOffset);
						}
						hookupPos.rotateCoarse(secondVehicle.angles).add(secondVehicle.position);
						
						if(hitchPos.distanceTo(hookupPos) < 10){
							boolean validType = firstConnection.type.equals(secondConnection.type);
							boolean validDistance = hitchPos.distanceTo(hookupPos) < 2;
							if(validType && validDistance){
								firstVehicle.changeTrailer(secondVehicle, firstConnection, secondConnection, optionalFirstPart, optionalSecondPart);
								return TrailerConnectionResult.TRAILER_CONNECTED;
							}else if(validType){
								matchingConnection = true;
							}else if(validDistance){
								trailerInRange = true;
							}
						}
					}
				}
			}
			
			if(matchingConnection && !trailerInRange){
				return TrailerConnectionResult.TRAILER_TOO_FAR;
			}else if(!matchingConnection && trailerInRange){
				return TrailerConnectionResult.TRAILER_WRONG_HITCH;
			}
		}
		return TrailerConnectionResult.NO_TRAILER_NEARBY;
	}
	
	/**
	 * Method block for connecting this vehicle to another vehicle.
	 * This vehicle will be considered towed by the other vehicle, and will
	 * do different physics to follow the other vehicle than it normally would.
	 * The passed-in vehicle should be the trailer we want to tow, or null if 
	 * we should be disconnecting from any trailer we are currently towing.
	 * Hitch and hookup index should be part of our and the trailer's respective
	 * definitions.
	 */
	public void changeTrailer(EntityVehicleD_Moving trailer, JSONConnection hitchConnection, JSONConnection hookupConnection, APart optionalHitchPart, APart optionalHookupPart){
		if(trailer == null){
			towedVehicle.towedByVehicle = null;
			towedVehicle.activeHookupConnection = null;
			towedVehicle.parkingBrakeOn = true;
			towedVehicle = null;
		}else{
			towedVehicle = (EntityVehicleF_Physics) trailer;
			activeHitchConnection = hitchConnection;
			activeHitchPart = optionalHitchPart;
			trailer.towedByVehicle = (EntityVehicleF_Physics) this;
			trailer.activeHookupConnection = hookupConnection; 
			trailer.activeHookupPart = optionalHookupPart;
			trailer.parkingBrakeOn = false;
			if(activeHitchConnection.mounted){
				trailer.angles.setTo(angles);
				trailer.prevAngles.setTo(prevAngles);
				if(activeHitchPart != null){
					trailer.angles.add(activeHitchPart.localAngles);
					trailer.prevAngles.add(activeHitchPart.localAngles);
				}
				EntityVehicleD_Moving trailerTrailer = trailer.towedVehicle;
				while(trailerTrailer != null){
					trailerTrailer.angles.setTo(angles);
					trailerTrailer.prevAngles.setTo(prevAngles);
					trailerTrailer = trailerTrailer.towedVehicle;
				}
			}
		}
		if(!world.isClient()){
			InterfacePacket.sendToAllClients(new PacketVehicleTrailerChange((EntityVehicleF_Physics) this, hitchConnection, hookupConnection, optionalHitchPart, optionalHookupPart));
		}
	}
	
	public void addToServerDeltas(Point3d motionAdded, Point3d rotationAdded){
		serverDeltaM.add(motionAdded);
		serverDeltaR.add(rotationAdded);
	}
	
	/**
	 * Method block for getting the steering angle of this vehicle.
	 * This returns the normalized steering angle, from -1.0 to 1.0;
	 */
	protected abstract float getSteeringAngle();
	
	/**
	 * Adds to the steering angle.  Passed-in value is the number
	 * of degrees to add.  Clamping may be applied if required.
	 * Note: this will only be called on the server from internal
	 * methods.  Clients should be sent a packet based on the
	 * actual state changes.
	 */
	protected abstract void addToSteeringAngle(float degrees);
	
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
		data.setBoolean("parkingBrakeOn", parkingBrakeOn);
		data.setInteger("brake", brake);
		if(towedVehicle != null){
			data.setString("towedVehicleSavedID", towedVehicle.uniqueUUID);
			if(activeHitchPart != null){
				data.setPoint3d("activeHitchPartSavedOffset", activeHitchPart.placementOffset);
				data.setInteger("activeHitchConnectionSavedIndex", activeHitchPart.definition.connections.indexOf(activeHitchConnection));
			}else{
				data.setInteger("activeHitchConnectionSavedIndex", definition.connections.indexOf(activeHitchConnection));
			}
		}
		if(towedByVehicle != null){
			data.setString("towedByVehicleSavedID", towedByVehicle.uniqueUUID);
			if(activeHookupPart != null){
				data.setPoint3d("activeHookupPartSavedOffset", activeHookupPart.placementOffset);
				data.setInteger("activeHookupConnectionSavedIndex", activeHookupPart.definition.connections.indexOf(activeHookupConnection));
			}else{
				data.setInteger("activeHookupConnectionSavedIndex", definition.connections.indexOf(activeHookupConnection));
			}
		}
		data.setPoint3d("serverDeltaM", serverDeltaM);
		data.setPoint3d("serverDeltaR", serverDeltaR);
	}
	
	/**
	 * Emum for easier functions for trailer connections.
	 */
	public static enum TrailerConnectionResult{
		NO_TRAILER_NEARBY,
		TRAILER_TOO_FAR,
		TRAILER_WRONG_HITCH,
		TRAILER_CONNECTED;
	}
}
