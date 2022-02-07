package minecrafttransportsimulator.entities.instances;

import java.util.Iterator;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.TrailerConnection;
import minecrafttransportsimulator.baseclasses.VehicleGroundDeviceCollection;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.tileentities.components.RoadFollowingState;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane.LaneSelectionRequest;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.jsondefs.JSONCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketVehicleServerMovement;
import minecrafttransportsimulator.systems.ConfigSystem;

/**At the final basic vehicle level we add in the functionality for state-based movement.
 * Here is where the functions for moving permissions, such as collision detection
 * routines and ground device effects come in.  We also add functionality to keep
 * servers and clients from de-syncing.  At this point we now have a basic vehicle
 *  that can be manipulated for movement in the world.  
 * 
 * @author don_bruce
 */
abstract class AEntityVehicleD_Moving extends AEntityVehicleC_Colliding{
	//Static variables used in logic that are kept in the global map.
	public static final String LEFTTURNLIGHT_VARIABLE = "left_turn_signal";
	public static final String RIGHTTURNLIGHT_VARIABLE = "right_turn_signal";
	public static final String BRAKE_VARIABLE = "brake";
	public static final String PARKINGBRAKE_VARIABLE = "p_brake";
	
	//External state control.
	@DerivedValue
	public double brake;
	@DerivedValue
	public boolean parkingBrakeOn;
	public static final double MAX_BRAKE = 1D;
	
	//Internal states.
	public boolean goingInReverse;
	public boolean slipping;
	public boolean skidSteerActive;
	public boolean lockedOnRoad;
	public double groundVelocity;
	public double weightTransfer = 0;
	
	//Properties
	@ModifiedValue
	public float currentDownForce;
	@ModifiedValue
	public float currentBrakingFactor;
	@ModifiedValue
	public float currentOverSteer;
	@ModifiedValue
	public float currentUnderSteer;
	
	//Road-following data.
	private RoadFollowingState frontFollower;
	private RoadFollowingState rearFollower;
	private LaneSelectionRequest selectedSegment = LaneSelectionRequest.NONE;
	private double totalPathDelta;
	private double prevTotalPathDelta;
	
	//Internal movement variables.
	private final Point3d serverDeltaM;
	private final Point3d serverDeltaR;
	private double serverDeltaP;
	private final Point3d clientDeltaM;
	private final Point3d clientDeltaR;
	private double clientDeltaP;
	private final Point3d clientDeltaMApplied = new Point3d();
	private final Point3d clientDeltaRApplied = new Point3d();
	private double clientDeltaPApplied;
	private final Point3d roadMotion = new Point3d();
	private final Point3d roadRotation = new Point3d();
	private final Point3d collisionMotion = new Point3d();
	private final Point3d collisionRotation = new Point3d();
	private final Point3d motionApplied = new Point3d();
	private final Point3d rotationApplied = new Point3d();
	private double pathingApplied;
	private final Point3d tempBoxPosition = new Point3d();
	private final Point3d tempBoxRotation = new Point3d();
	private final Point3d normalizedGroundVelocityVector = new Point3d();
	private final Point3d normalizedGroundHeadingVector = new Point3d();
	private AEntityE_Interactable<?> lastCollidedEntity;
  	public VehicleGroundDeviceCollection groundDeviceCollective;
	
	public AEntityVehicleD_Moving(WrapperWorld world, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, placingPlayer, data);
		this.totalPathDelta = data.getDouble("totalPathDelta");
		this.prevTotalPathDelta = totalPathDelta;
		this.serverDeltaM = data.getPoint3d("serverDeltaM");
		this.serverDeltaR = data.getPoint3d("serverDeltaR");
		this.serverDeltaP = data.getDouble("serverDeltaP");
		this.clientDeltaM = serverDeltaM.copy();
		this.clientDeltaR = serverDeltaR.copy();
		this.clientDeltaP = serverDeltaP;
		this.groundDeviceCollective = new VehicleGroundDeviceCollection((EntityVehicleF_Physics) this);
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			world.beginProfiling("VehicleD_Level", true);
			
			//If we were placed down, and this is our first tick, check our collision boxes to make sure we are't in the ground.
			if(ticksExisted == 1 && placingPlayer != null && !world.isClient()){
				//Get how far above the ground the vehicle needs to be, and move it to that position.
				//First boost Y based on collision boxes.
				double furthestDownPoint = 0;
				for(JSONCollisionGroup collisionGroup : definition.collisionGroups){
					for(JSONCollisionBox collisionBox : collisionGroup.collisions){
						furthestDownPoint = Math.min(collisionBox.pos.y - collisionBox.height/2F, furthestDownPoint);
					}
				}
				
				//Next, boost based on parts.
				for(APart part : parts){
					furthestDownPoint = Math.min(part.placementOffset.y - part.getHeight()/2F, furthestDownPoint);
				}
				
				//Add on -0.1 blocks for the default collision clamping.
				//This prevents the clamping of the collision boxes from hitting the ground if they were clamped.
				furthestDownPoint += -0.1;
				
				//Apply the boost, and check collisions.
				//If the core collisions are colliding, set the vehicle as dead and abort.
				//We need to update the boxes first, however, as they haven't been updated yet.
				motionApplied.set(0, -furthestDownPoint, 0);
				rotationApplied.set(0, 0, 0);
				position.add(motionApplied);
				for(BoundingBox coreBox : allBlockCollisionBoxes){
					coreBox.updateToEntity(this, null);
					if(coreBox.updateCollidingBlocks(world, new Point3d(0D, -furthestDownPoint, 0D))){
						//New vehicle shouldn't have been spawned.  Bail out.
						remove();
						placingPlayer.sendPacket(new PacketPlayerChatMessage(placingPlayer, "interact.failure.nospace"));
						//Need to add stack back as it will have been removed here.
						if(!placingPlayer.isCreative()){
							placingPlayer.setHeldStack(getItem().getNewStack(save(new WrapperNBT())));
						}
						return false;
					}else{
						//Update deltas and send to clients.
						addToServerDeltas(motionApplied, rotationApplied, pathingApplied);
						InterfacePacket.sendToAllClients(new PacketVehicleServerMovement((EntityVehicleF_Physics) this, motionApplied, rotationApplied, pathingApplied));
					}
				}
			}
			
			//Update brake status.  This is used in a lot of locations, so we don't want to query the set every time.
			brake = getVariable(BRAKE_VARIABLE);
			parkingBrakeOn = isVariableActive(PARKINGBRAKE_VARIABLE);
			
			//Update our GDB members if any of our ground devices don't have the same total offset as placement.
			//This is required to move the GDBs if the GDs move.
			world.beginProfiling("GroundDevices", true);
			if(ticksExisted == 1){
				groundDeviceCollective.updateBounds();
			}
			for(APart part : parts){
				if(part instanceof PartGroundDevice){
					if(part.prevActive != part.isActive){
						groundDeviceCollective.updateMembers();
						groundDeviceCollective.updateBounds();
						break;
					}
					if(!part.localOffset.equals(part.prevLocalOffset)){
						groundDeviceCollective.updateBounds();
						break;
					}
				}
			}
			
			//Now do update calculations and logic.
			if(!ConfigSystem.configObject.general.noclipVehicles.value || groundDeviceCollective.isReady()){
				world.beginProfiling("GroundForces", false);
				getForcesAndMotions();
				world.beginProfiling("GroundOperations", false);
				performGroundOperations();
				world.beginProfiling("TotalMovement", false);
				moveVehicle();
				if(!world.isClient()){
					adjustControlSurfaces();
				}
			}
			
			//Update parts after all movement is done.
			world.beginProfiling("PostMovement", false);
			updatePostMovement();
			world.endProfiling();
			world.endProfiling();
			return true;
		}else{
			return false;
		}
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
	protected void sortBoxes(){
		super.sortBoxes();
		if(ticksExisted == 1){
			//Need to do initial GDB updates.
			groundDeviceCollective.updateMembers();
			groundDeviceCollective.updateBounds();
			groundDeviceCollective.updateCollisions();
		}
	}
	
	@Override
	public boolean needsChunkloading(){
		return rearFollower != null || (towedByConnection != null && ((AEntityVehicleD_Moving) towedByConnection.hitchVehicle).rearFollower != null);
	}
	
	@Override
	public boolean canCollideWith(AEntityB_Existing entityToCollide){
		if(towedByConnection != null && entityToCollide.equals(towedByConnection.hitchVehicle)){
			return false;
		}else if(!towingConnections.isEmpty()){
			for(TrailerConnection connection : towingConnections){
				if(entityToCollide.equals(connection.hookupVehicle)){
					return false;
				}
			}
		}
		return super.canCollideWith(entityToCollide); 
	}
	
	@Override
	public void connectAsTrailer(TrailerConnection connection){
		super.connectAsTrailer(connection);
		if(parkingBrakeOn){
			toggleVariable(PARKINGBRAKE_VARIABLE);
		}
		setVariable(BRAKE_VARIABLE, 0);
		frontFollower = null;
		rearFollower = null;
	}
	
	@Override
	public void disconnectAsTrailer(){
		super.disconnectAsTrailer();
		if(definition.motorized.isTrailer){
			parkingBrakeOn = true;
		}
	}
	
	/**
	 * Returns the follower for the rear of the vehicle.  Front follower should
	 * be obtained by getting the point from this follower the distance away from the
	 * front and the rear position.  This may be the same curve, this may not.
	 */
	private RoadFollowingState getFollower(){
		Point3d contactPoint = groundDeviceCollective.getContactPoint(false);
		if(contactPoint != null){
			contactPoint.rotateFine(angles).add(position);
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
		float brakingFactor = towedByConnection == null ? getBrakingForce()*currentBrakingFactor : 0;
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
			if (this.towedByConnection == null){
				double overSteerForce = Math.max(velocity / 4, 1);
				if (definition.motorized.overSteerAccel != 0){
					weightTransfer += ((motion.dotProduct(motion) - prevMotion.dotProduct(prevMotion)) * weightTransfer) * currentOverSteer;
					if (Math.abs(weightTransfer) > Math.abs(definition.motorized.overSteerAccel) && Math.abs(weightTransfer) > Math.abs(definition.motorized.overSteerDecel)){
			    			weightTransfer = definition.motorized.overSteerAccel;
					}else if(Math.abs(weightTransfer) < Math.abs(definition.motorized.overSteerDecel) && weightTransfer < Math.abs(definition.motorized.overSteerAccel)){
						weightTransfer = definition.motorized.overSteerDecel;
					}
				}else{
					weightTransfer = currentOverSteer;
				}
				rotation.y += crossProduct.y * weightTransfer + (Math.abs(crossProduct.y) * -currentUnderSteer * turningForce) * overSteerForce;
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
				
				//If we are slipping while turning, set the slipping variable to let other systems know.
				//Only do this as a main vehicle.  If we are a trailer, we don't do this unless the vehicle towing us is slipping.
				slipping = towedByConnection == null ? (world.isClient() && motionFactor != 1 && velocity > 0.75) : (towedByConnection != null && towedByConnection.hitchVehicle.slipping);
			}
		}
	}
	
	/**
	 * Returns force for braking.
	 * Depends on number of grounded core collision sections and braking ground devices.
	 */
	private float getBrakingForce(){
		double brakingPower = parkingBrakeOn ? MAX_BRAKE : brake;
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
			if(brakingPower > 0){
				brakingFactor += 0.15D*brakingPower*groundDeviceCollective.getNumberBoxesInLiquid();
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
			skiddingFactor += Math.max(groundDevice.getLateralFriction() - groundDevice.getFrictionLoss(), 0);
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
		double steeringAngle = getSteeringAngle()*45;
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
								if(((PartEngine) part).currentGear == 0 && ((PartEngine) part).running){
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
					turningForce *= Math.pow(0.3F, (groundVelocity*(1 - currentDownForce) - 0.35D));
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
		world.beginProfiling("GDBInit", true);
		collidedEntities.clear();
		groundDeviceCollective.updateCollisions();
		
		//If we aren't on a road, try to find one.
		//If we are an aircraft, don't check as we shouldn't have aircraft on roads.
		//If we are being towed, only check if the towing vehicle is on the road (since we should be on one too). 
		world.beginProfiling("RoadChecks", false);
		if(definition.motorized.isAircraft || (towedByConnection != null && !towedByConnection.hitchVehicle.lockedOnRoad)){
			frontFollower = null;
			rearFollower = null;
		}else if((frontFollower == null || rearFollower == null) && ticksExisted%20 == 0){
			Point3d frontContact = groundDeviceCollective.getContactPoint(true);
			Point3d rearContact = groundDeviceCollective.getContactPoint(false);
			if(frontContact != null && rearContact != null){
				rearFollower = getFollower();
				//If we are being towed, and we got followers, adjust them to our actual position.
				//This is because we might have connected to the vehicle this tick, but won't be aligned
				//to our towed position as connections are exact.
				if(rearFollower != null){
					float pointDelta = (float) rearContact.distanceTo(frontContact);
					if(towedByConnection == null){
						frontFollower = new RoadFollowingState(rearFollower.lane, rearFollower.curve, rearFollower.goingForwards, rearFollower.currentSegment).updateCurvePoints(pointDelta, LaneSelectionRequest.NONE);
					}else{
						float segmentDelta = (float) (towedByConnection.getHitchCurrentPosition().subtract(towedByConnection.hitchVehicle.position).length() + towedByConnection.getHookupCurrentPosition().subtract(towedByConnection.hookupVehicle.position).length());
						if(towedByConnection.hitchEntity instanceof APart ? ((APart) towedByConnection.hitchEntity).localOffset.z <= 0 : towedByConnection.hitchConnection.pos.z <= 0){
							segmentDelta = -segmentDelta;
						}
						rearFollower = new RoadFollowingState(((AEntityVehicleD_Moving) towedByConnection.hitchVehicle).rearFollower);
						rearFollower.updateCurvePoints(segmentDelta, ((AEntityVehicleD_Moving) towedByConnection.hitchVehicle).selectedSegment);
						frontFollower = new RoadFollowingState(rearFollower);
						frontFollower.updateCurvePoints(pointDelta, ((AEntityVehicleD_Moving) towedByConnection.hitchVehicle).selectedSegment);
					}
				}
			}
		}
		
		//If we are on a road, we need to bypass the logic for pitch/yaw/roll checks, and GDB checks.
		//This is because if we are on a road we need to follow the road's curve.
		//If we have both followers, do road-following logic.
		//If we don't, or we're turning off the road, do normal vehicle logic.
		roadMotion.set(0, 0, 0);
		roadRotation.set(0, 0, 0);
		if(frontFollower != null && rearFollower != null){
			world.beginProfiling("RoadOperations", false);
			
			//Check for the potential to change the requested segment.
			//We can only do this if both our followers are on the same segment.
			LaneSelectionRequest requestedSegment;
			if(!(isVariableActive(LEFTTURNLIGHT_VARIABLE) ^ isVariableActive(RIGHTTURNLIGHT_VARIABLE))){
				requestedSegment = LaneSelectionRequest.NONE;
			}else if(isVariableActive(LEFTTURNLIGHT_VARIABLE)){
				requestedSegment = goingInReverse ? LaneSelectionRequest.RIGHT : LaneSelectionRequest.LEFT;
			}else{
				requestedSegment = goingInReverse ? LaneSelectionRequest.LEFT : LaneSelectionRequest.RIGHT;
			}
			if(frontFollower.equals(rearFollower)){
				selectedSegment = requestedSegment;
			}
			
			float segmentDelta = (float) (totalPathDelta - prevTotalPathDelta);
			prevTotalPathDelta = totalPathDelta;
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
						addToSteeringAngle((float) (goingInReverse ? -roadRotation.y : roadRotation.y)*1.5F);
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
		lockedOnRoad = frontFollower != null && rearFollower != null; 
		//If followers aren't valid, do normal logic.
		if(!lockedOnRoad){
			//If any ground devices are collided after our movement, apply corrections to prevent this.
			//The first correction we apply is +y motion.  This counteracts gravity, and any GDBs that may
			//have been moved into the ground by the application of our motion and rotation.  We do this before collision
			//boxes, as we don't want gravity to cause us to move into something when we really shouldn't move down because
			//all the GDBs prevent this.  In either case, apply +y motion to get all the GDBs out of the ground.
			//This may not be possible, however, if the boxes are too deep into the ground.  We don't want vehicles to
			//instantly climb mountains.  Because of this, we add only 1/8 block, or enough motionY to prevent collision,
			//whichever is the lower of the two.  If we apply boost, update our collision boxes before the next step.
			//Note that this logic is not applied on trailers, as they use special checks with only rotations for movement.
			if(towedByConnection == null){
				world.beginProfiling("GroundBoostCheck", false);
				groundCollisionBoost = groundDeviceCollective.getMaxCollisionDepth()/SPEED_FACTOR;
				if(groundCollisionBoost > 0){
					world.beginProfiling("GroundBoostApply", false);
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
			world.beginProfiling("CollisionCheck_" + allBlockCollisionBoxes.size(), false);
			if(isCollisionBoxCollided()){
				world.beginProfiling("CollisionHandling", false);
				if(towedByConnection != null){
					Point3d initalMotion = motion.copy();
					if(correctCollidingMovement()){
						return;
					}
					towedByConnection.hitchVehicle.motion.add(motion).subtract(initalMotion);
				}else if(correctCollidingMovement()){
					return;
				}
			}else if(towedByConnection == null || !towedByConnection.hitchConnection.mounted){
				world.beginProfiling("GroundHandlingPitch", false);
				groundRotationBoost = groundDeviceCollective.performPitchCorrection(groundCollisionBoost);
				//Don't do roll correction if we don't have roll.
				if(groundDeviceCollective.canDoRollChecks()){
					world.beginProfiling("GroundHandlingRoll", false);
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
		
		//If we collided with any entities, move us with them.
		//This allows for transports without mounting.
		if(!collidedEntities.isEmpty()){
			world.beginProfiling("EntityMoveAlong", false);
			for(AEntityE_Interactable<?> interactable : collidedEntities){
				if(interactable instanceof AEntityVehicleD_Moving){
					AEntityVehicleD_Moving mainVehicle = (AEntityVehicleD_Moving) interactable;
					//Set angluar movement delta.
					collisionRotation.setTo(mainVehicle.angles).subtract(mainVehicle.prevAngles);
					
					//Get vector from collided box to this entity.
					Point3d centerOffset = position.copy().subtract(mainVehicle.prevPosition);
					
					//Add rotation contribution to offset.
					collisionMotion.setTo(centerOffset).rotateFine(collisionRotation).subtract(centerOffset);
					
					//Add linear contribution to offset.
					collisionMotion.add(mainVehicle.position).subtract(mainVehicle.prevPosition);
					
					//If we just contacted an entity, adjust our motion to match that entity's motion.
					//We take our motion, and then remove it so it's the delta to that entity.
					//This ensures that if we're moving and land on an entity, we don't run off.
					if(lastCollidedEntity == null){
						lastCollidedEntity = interactable;
						motion.subtract(lastCollidedEntity.motion);
					}
					
					//Only check one for now.  We could do multiple, but then we'd have to do maths.
					break;
				}
			}
		}else{
			if(lastCollidedEntity != null){
				//Add-back to our motion by adding the entity's motion.
				motion.add(lastCollidedEntity.motion);
				lastCollidedEntity = null;
			}
		}

		//Now that that the movement has been checked, move the vehicle.
		world.beginProfiling("ApplyMotions", false);
		motionApplied.setTo(motion).multiply(SPEED_FACTOR).add(roadMotion).add(collisionMotion);
		rotationApplied.setTo(rotation).add(roadRotation).add(collisionRotation);
		if(lockedOnRoad){
			if(towedByConnection != null){
				pathingApplied = ((AEntityVehicleD_Moving) towedByConnection.hitchVehicle).pathingApplied;
			}else{
				pathingApplied = goingInReverse ? -velocity*SPEED_FACTOR : velocity*SPEED_FACTOR;
			}
		}else{
			pathingApplied = 0;
		}
		collisionMotion.set(0, 0, 0);
		collisionRotation.set(0, 0, 0);
		if(!world.isClient()){
			if(!motionApplied.isZero() || !rotationApplied.isZero()){
				addToServerDeltas(motionApplied, rotationApplied, pathingApplied);
				InterfacePacket.sendToAllClients(new PacketVehicleServerMovement((EntityVehicleF_Physics) this, motionApplied, rotationApplied, pathingApplied));
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
				
				clientDeltaPApplied = serverDeltaP - clientDeltaP;
				clientDeltaPApplied *= Math.abs(clientDeltaPApplied);
				clientDeltaPApplied *= 1D/25D;
				if(clientDeltaPApplied > 5){
					clientDeltaPApplied = 5;
				}
				pathingApplied += clientDeltaPApplied;
				
				//Add actual movement to client deltas to prevent further corrections.
				clientDeltaM.add(motionApplied);
				clientDeltaR.add(rotationApplied);
				clientDeltaP += pathingApplied;
			}
		}
		
		//Now add actual position and angles.
		position.add(motionApplied);
		angles.add(rotationApplied);
		totalPathDelta += pathingApplied;
		orientation.axis.set(0, 0, 1).rotateFine(angles);
		orientation.updateQuaternion(false);
		
		//Before we end this tick we need to remove any motions added for ground devices.  These motions are required 
		//only for the updating of the vehicle position due to rotation operations and should not be considered forces.
		//Leaving them in will cause the physics system to think a force was applied, which will make it behave badly!
		//We need to strip away any positive motion.y we gave the vehicle to get it out of the ground if it
		//collided on its ground devices, as well as any motion.y we added when doing rotation adjustments.
		motion.y -= (groundCollisionBoost + groundRotationBoost);
		world.endProfiling();
	}
	
	/**
	 *  Checks if we have a collided collision box.  If so, true is returned.
	 */
	private boolean isCollisionBoxCollided(){
		if(motion.length() > 0.001){
			boolean clearedCache = false;
			for(BoundingBox box : allBlockCollisionBoxes){
				tempBoxPosition.setTo(box.globalCenter).subtract(position).rotateFine(rotation).subtract(box.globalCenter).add(position).addScaled(motion, SPEED_FACTOR);
				if(!box.collidesWithLiquids && world.checkForCollisions(box, tempBoxPosition, !clearedCache)){
					return true;
				}
				clearedCache = true;
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
				}else if(collisionDepth == -2){
					break;
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
				}else if(collisionDepth == -2){
					break;
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
				}else if(collisionDepth == -2){
					break;
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
					tempBoxPosition.setTo(box.globalCenter).subtract(position).rotateFine(tempBoxRotation).add(position).add(motion.x*SPEED_FACTOR, motion.y*SPEED_FACTOR, motion.z*SPEED_FACTOR);
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
					tempBoxPosition.setTo(box.globalCenter).subtract(position).rotateFine(tempBoxRotation).add(position).add(motion.x*SPEED_FACTOR, motion.y*SPEED_FACTOR, motion.z*SPEED_FACTOR);
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
					tempBoxPosition.setTo(box.globalCenter).subtract(position).rotateFine(tempBoxRotation).add(position).add(motion.x*SPEED_FACTOR, motion.y*SPEED_FACTOR, motion.z*SPEED_FACTOR);
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
	
	public void addToServerDeltas(Point3d motionAdded, Point3d rotationAdded, double pathingAdded){
		serverDeltaM.add(motionAdded);
		serverDeltaR.add(rotationAdded);
		serverDeltaP += pathingAdded;
	}
	
	/**
	 * Method block for getting the steering angle of this vehicle.
	 * This returns the normalized steering angle, from -1.0 to 1.0;
	 */
	protected abstract double getSteeringAngle();
	
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
	protected abstract void adjustControlSurfaces();
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setPoint3d("serverDeltaM", serverDeltaM);
		data.setPoint3d("serverDeltaR", serverDeltaR);
		data.setDouble("serverDeltaP", serverDeltaP);
		return data;
	}
}
