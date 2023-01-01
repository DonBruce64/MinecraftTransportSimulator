package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TowingConnection;
import minecrafttransportsimulator.baseclasses.VehicleGroundDeviceCollection;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.tileentities.components.RoadFollowingState;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane.LaneSelectionRequest;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.jsondefs.JSONCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketVehicleServerMovement;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * At the final basic vehicle level we add in the functionality for state-based movement.
 * Here is where the functions for moving permissions, such as collision detection
 * routines and ground device effects come in.  We also add functionality to keep
 * servers and clients from de-syncing.  At this point we now have a basic vehicle
 * that can be manipulated for movement in the world.
 *
 * @author don_bruce
 */
abstract class AEntityVehicleD_Moving extends AEntityVehicleC_Colliding {
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
    private boolean updateGroundDevicesRequest;
    public double groundVelocity;
    public double weightTransfer = 0;
    public final RotationMatrix rotation = new RotationMatrix();
    private final IWrapperPlayer placingPlayer;

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
    protected RoadFollowingState frontFollower;
    protected RoadFollowingState rearFollower;
    private LaneSelectionRequest selectedSegment = LaneSelectionRequest.NONE;
    private double totalPathDelta;
    private double prevTotalPathDelta;
    private boolean invertedRoadOrientation;

    //Internal movement variables.
    private final Point3D serverDeltaM;
    private final Point3D serverDeltaR;
    private double serverDeltaP;
    private final Point3D serverDeltaMApplied = new Point3D();
    private final Point3D serverDeltaRApplied = new Point3D();
    private double serverDeltaPApplied;

    private final Point3D clientDeltaM;
    private final Point3D clientDeltaR;
    private double clientDeltaP;
    private final Point3D clientDeltaMApplied = new Point3D();
    private final Point3D clientDeltaRApplied = new Point3D();
    private double clientDeltaPApplied;

    private final Point3D roadMotion = new Point3D();
    private final Point3D roadRotation = new Point3D();
    private final Point3D vehicleCollisionMotion = new Point3D();
    private final RotationMatrix vehicleCollisionRotation = new RotationMatrix();
    private final Point3D groundMotion = new Point3D();
    private final Point3D motionApplied = new Point3D();
    private final RotationMatrix rotationApplied = new RotationMatrix();
    private double pathingApplied;

    private final Point3D tempBoxPosition = new Point3D();
    private final Point3D normalizedGroundVelocityVector = new Point3D();
    private final Point3D normalizedGroundHeadingVector = new Point3D();
    private AEntityE_Interactable<?> lastCollidedEntity;
    public VehicleGroundDeviceCollection groundDeviceCollective;

    public AEntityVehicleD_Moving(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data) {
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
        this.placingPlayer = placingPlayer;
    }

    @Override
    public void update() {
        super.update();
        world.beginProfiling("VehicleD_Level", true);
        //If we were placed down, and this is our first tick, check our collision boxes to make sure we are't in the ground.
        if (ticksExisted == 1 && placingPlayer != null && !world.isClient()) {
            //Get how far above the ground the vehicle needs to be, and move it to that position.
            //First boost Y based on collision boxes.
            double furthestDownPoint = 0;
            for (JSONCollisionGroup collisionGroup : definition.collisionGroups) {
                for (JSONCollisionBox collisionBox : collisionGroup.collisions) {
                    furthestDownPoint = Math.min(collisionBox.pos.y - collisionBox.height / 2F, furthestDownPoint);
                }
            }

            //Next, boost based on parts.
            for (APart part : allParts) {
                furthestDownPoint = Math.min(part.placementDefinition.pos.y - part.getHeight() / 2F, furthestDownPoint);
            }

            //Add on -0.1 blocks for the default collision clamping.
            //This prevents the clamping of the collision boxes from hitting the ground if they were clamped.
            furthestDownPoint -= 0.1;

            //Apply the boost, and check collisions.
            //If the core collisions are colliding, set the vehicle as dead and abort.
            //We need to update the boxes first, however, as they haven't been updated yet.
            motionApplied.set(0, -furthestDownPoint, 0);
            rotationApplied.angles.set(0, 0, 0);
            position.add(motionApplied);
            for (BoundingBox coreBox : allBlockCollisionBoxes) {
                coreBox.updateToEntity(this, null);
                if (coreBox.updateCollidingBlocks(world, new Point3D(0D, -furthestDownPoint, 0D))) {
                    //New vehicle shouldn't have been spawned.  Bail out.
                    placingPlayer.sendPacket(new PacketPlayerChatMessage(placingPlayer, JSONConfigLanguage.INTERACT_VEHICLE_NOSPACE));
                    //Need to add stack back as it will have been removed here.
                    if (!placingPlayer.isCreative()) {
                        placingPlayer.setHeldStack(getItem().getNewStack(save(InterfaceManager.coreInterface.getNewNBTWrapper())));
                    }
                    remove();
                    return;
                } else {
                    addToServerDeltas(null, null, 0);
                }
            }
        }

        //Update brake status.  This is used in a lot of locations, so we don't want to query the set every time.
        brake = getVariable(BRAKE_VARIABLE);
        parkingBrakeOn = isVariableActive(PARKINGBRAKE_VARIABLE);

        //Now do update calculations and logic.
        if (!ConfigSystem.settings.general.noclipVehicles.value || groundDeviceCollective.isReady()) {
            world.beginProfiling("GroundForces", false);
            getForcesAndMotions();
            world.beginProfiling("GroundOperations", false);
            if (towedByConnection == null || !towedByConnection.hitchConnection.mounted) {
                performGroundOperations();
            }
            world.beginProfiling("TotalMovement", false);
            moveVehicle();
            if (!world.isClient()) {
                adjustControlSurfaces();
            }
        }
        world.endProfiling();
    }

    @Override
    protected void updateAllpartList() {
        super.updateAllpartList();
        if (ticksExisted > 1) {
            updateGroundDevicesRequest = true;
        }
    }

    @Override
    protected void updateEncompassingBox() {
        super.updateEncompassingBox();
        if (ticksExisted == 1 || updateGroundDevicesRequest) {
            groundDeviceCollective.updateMembers();
            groundDeviceCollective.updateBounds();
            groundDeviceCollective.updateCollisions();
            updateGroundDevicesRequest = false;
        }
    }

    @Override
    public void connectTrailer(TowingConnection connection) {
        super.connectTrailer(connection);
        AEntityVehicleD_Moving towedVehicle = connection.towedVehicle;
        if (towedVehicle.parkingBrakeOn) {
            towedVehicle.setVariable(PARKINGBRAKE_VARIABLE, 0);
        }
        towedVehicle.setVariable(BRAKE_VARIABLE, 0);
        towedVehicle.frontFollower = null;
        towedVehicle.rearFollower = null;
    }

    @Override
    public void disconnectTrailer(int connectionIndex) {
        TowingConnection connection = towingConnections.get(connectionIndex);
        if (connection.towedVehicle.definition.motorized.isTrailer) {
            connection.towedVehicle.setVariable(PARKINGBRAKE_VARIABLE, 1);
        }
        super.disconnectTrailer(connectionIndex);
    }

    /**
     * Returns the follower for the rear of the vehicle.  Front follower should
     * be obtained by getting the point from this follower the distance away from the
     * front and the rear position.  This may be the same curve, this may not.
     */
    private RoadFollowingState getFollower() {
        Point3D contactPoint = groundDeviceCollective.getContactPoint(false);
        if (contactPoint != null) {
            contactPoint.rotate(orientation).add(position);
            ABlockBase block = world.getBlock(contactPoint);
            if (block instanceof BlockCollision) {
                TileEntityRoad road = ((BlockCollision) block).getMasterRoad(world, contactPoint);
                if (road != null) {
                    //Check to see which lane we are on, if any.
                    Point3D testPoint = new Point3D();
                    Point3D testRotation;
                    for (RoadLane lane : road.lanes) {
                        //Check path-points on the curve.  If our angles and position are close, set this as the curve.
                        for (BezierCurve curve : lane.curves) {
                            for (float f = 0; f < curve.pathLength; ++f) {
                                curve.setPointToPositionAt(testPoint, f);
                                if (testPoint.isDistanceToCloserThan(contactPoint, 1)) {
                                    testRotation = curve.getRotationAt(f).angles;
                                    boolean sameDirection = Math.abs(testRotation.getClampedYDelta(orientation.angles.y)) < 10;
                                    boolean oppositeDirection = Math.abs(testRotation.getClampedYDelta(orientation.angles.y)) > 170;
                                    if (sameDirection || oppositeDirection) {
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
    private void performGroundOperations() {
        //Get braking force and apply it to the motions.
        float brakingFactor = towedByConnection == null ? getBrakingForce() * currentBrakingFactor : 0;
        if (brakingFactor > 0) {
            double brakingForce = 20F * brakingFactor / currentMass;
            if (brakingForce > velocity) {
                motion.x = 0;
                motion.z = 0;
                rotation.angles.y = 0;
            } else {
                motion.x -= brakingForce * motion.x / velocity;
                motion.z -= brakingForce * motion.z / velocity;
            }
        }

        //Add rotation based on our turning factor, and then re-set ground states.
        //For turning, we keep turning momentum if the wheels are turned.
        normalizedGroundVelocityVector.set(motion.x, 0D, motion.z);
        groundVelocity = normalizedGroundVelocityVector.length();
        normalizedGroundVelocityVector.normalize();
        normalizedGroundHeadingVector.set(headingVector.x, 0D, headingVector.z);
        normalizedGroundHeadingVector.normalize();
        double turningForce = getTurningForce();
        double dotProduct = normalizedGroundVelocityVector.dotProduct(normalizedGroundHeadingVector, true);
        //TODO having velocity in the formula here has the potential to lead to hang-ups. Use packets perhaps?
        if (skidSteerActive) {
            goingInReverse = false;
        } else {
            if (!goingInReverse && dotProduct < -0.75 && (turningForce == 0 || velocity < 0.1)) {
                goingInReverse = true;
            } else if (goingInReverse && dotProduct > 0.75 && (turningForce == 0 || velocity < 0.1)) {
                goingInReverse = false;
            }
        }
        if (turningForce != 0) {
            rotation.angles.y += goingInReverse ? -turningForce : turningForce;
        }
        //Check how much grip the wheels have.
        float skiddingFactor = getSkiddingForce();
        if (skiddingFactor != 0 && groundVelocity > 0.01) {
            //Have enough grip, get angle delta between heading and motion.
            Point3D crossProduct = normalizedGroundVelocityVector.crossProduct(normalizedGroundHeadingVector);
            double vectorDelta = Math.toDegrees(Math.atan2(crossProduct.y, dotProduct));
            //Check if we are backwards and adjust our delta angle if so.
            if (goingInReverse && dotProduct < 0) {
                if (vectorDelta >= 90) {
                    vectorDelta = -(180 - vectorDelta);
                } else if (vectorDelta <= -90) {
                    vectorDelta = 180 + vectorDelta;
                }
            }
            if (this.towedByConnection == null) {
                double overSteerForce = Math.max(velocity / 4, 1);
                if (definition.motorized.overSteerAccel != 0) {
                    weightTransfer += ((motion.dotProduct(motion, false) - prevMotion.dotProduct(prevMotion, false)) * weightTransfer) * currentOverSteer;
                    if (Math.abs(weightTransfer) > Math.abs(definition.motorized.overSteerAccel) && Math.abs(weightTransfer) > Math.abs(definition.motorized.overSteerDecel)) {
                        weightTransfer = definition.motorized.overSteerAccel;
                    } else if (Math.abs(weightTransfer) < Math.abs(definition.motorized.overSteerDecel) && weightTransfer < Math.abs(definition.motorized.overSteerAccel)) {
                        weightTransfer = definition.motorized.overSteerDecel;
                    }
                } else {
                    weightTransfer = currentOverSteer;
                }
                rotation.angles.y += crossProduct.y * weightTransfer + (Math.abs(crossProduct.y) * -currentUnderSteer * turningForce) * overSteerForce;
            }

            //If we are offset, adjust our angle.
            if (Math.abs(vectorDelta) > 0.001) {
                //Get factor of how much we can correct our turning.
                double motionFactor;
                if (vectorDelta > skiddingFactor) {
                    motionFactor = skiddingFactor / vectorDelta;
                } else if (vectorDelta < -skiddingFactor) {
                    motionFactor = -skiddingFactor / vectorDelta;
                } else {
                    motionFactor = 1;
                }

                //Apply motive changes to the vehicle based on how much we can turn it.
                //We basically take the two components of the motion, and apply one or the other depending on
                //how much delta the vector says we can change.
                Point3D idealMotion = goingInReverse ? normalizedGroundHeadingVector.copy().scale(-groundVelocity) : normalizedGroundHeadingVector.copy().scale(groundVelocity);
                idealMotion.scale(motionFactor).add(motion.x * (1 - motionFactor), 0D, motion.z * (1 - motionFactor));
                motion.x = idealMotion.x;
                motion.z = idealMotion.z;

                //If we are slipping while turning, set the slipping variable to let other systems know.
                //Only do this as a main vehicle.  If we are a trailer, we don't do this unless the vehicle towing us is slipping.
                slipping = towedByConnection == null ? (world.isClient() && motionFactor != 1 && velocity > 0.75) : towedByConnection.towingVehicle.slipping;
            }
        }
    }

    /**
     * Returns force for braking.
     * Depends on number of grounded core collision sections and braking ground devices.
     */
    private float getBrakingForce() {
        double brakingPower = parkingBrakeOn ? MAX_BRAKE : brake;
        float brakingFactor = 0;
        //First get the ground device braking contributions.
        //This is both grounded ground devices, and liquid collision boxes that are set as such.
        if (brakingPower > 0) {
            for (PartGroundDevice groundDevice : groundDeviceCollective.groundedGroundDevices) {
                float groundDevicePower = groundDevice.getMotiveFriction();
                if (groundDevicePower != 0) {
                    brakingFactor += Math.max(groundDevicePower - groundDevice.getFrictionLoss(), 0);
                }
            }
            if (brakingPower > 0) {
                brakingFactor += 0.15D * brakingPower * groundDeviceCollective.getNumberBoxesInLiquid();
            }
        }
        return brakingFactor;
    }

    /**
     * Returns force for skidding based on lateral friction and velocity.
     * If the value is non-zero, it indicates that yaw changes from ground
     * device calculations should be applied due to said devices being in
     * contact with the ground.
     */
    private float getSkiddingForce() {
        float skiddingFactor = 0;
        //First check grounded ground devices.
        for (PartGroundDevice groundDevice : groundDeviceCollective.groundedGroundDevices) {
            skiddingFactor += Math.max(groundDevice.getLateralFriction() - groundDevice.getFrictionLoss(), 0);
        }

        //Now check if any collision boxes are in liquid.  Needed for maritime vehicles.
        skiddingFactor += 0.5D * groundDeviceCollective.getNumberBoxesInLiquid();
        return skiddingFactor > 0 ? skiddingFactor : 0;
    }

    /**
     * Returns force for turning based on lateral friction, velocity, and wheel distance.
     * Sign of returned value indicates which direction entity should yaw.
     * A 0 value indicates no yaw change.
     */
    private double getTurningForce() {
        double steeringAngle = getSteeringAngle() * 45;
        skidSteerActive = false;
        if (steeringAngle != 0) {
            double furthestFrontPoint = 0;
            double furthestRearPoint = 0;
            
            double turningDistance = 0;
            //Check grounded ground devices for turn contributions.
            //Their distance from the center of the vehicle defines our turn arc.
            //Don't use fake ground devices here as it'll mess up math for vehicles.
            for (PartGroundDevice groundDevice : groundDeviceCollective.groundedGroundDevices) {
                if (groundDevice.wheelbasePoint.z > furthestFrontPoint) {
                    furthestFrontPoint = groundDevice.wheelbasePoint.z;
                }
                if (groundDevice.wheelbasePoint.z < furthestRearPoint) {
                    furthestRearPoint = groundDevice.wheelbasePoint.z;
                }
                turningDistance = furthestFrontPoint - furthestRearPoint;
            }

            //If we didn't find any ground devices to make us turn, check propellers in the water.
            if (turningDistance == 0) {
                for (APart part : allParts) {
                    if (part instanceof PartPropeller) {
                        if (part.isInLiquid()) {
                            turningDistance = Math.max(turningDistance, Math.abs(part.placementDefinition.pos.z));
                            break;
                        }
                    }
                }
            }

            //If we are able to turn, calculate the force we create to do so.
            if (turningDistance > 0) {
                //If we are vehicle that can do skid-steer, and that's active, do so now.
                if (definition.motorized.hasSkidSteer) {
                    if (groundDeviceCollective.isReady() && groundVelocity < 0.05) {
                        boolean foundNeutralEngine = false;
                        boolean leftWheelGrounded = false;
                        boolean rightWheelGrounded = false;
                        for (APart part : parts) {
                            if (part instanceof PartGroundDevice) {
                                if (groundDeviceCollective.groundedGroundDevices.contains(part)) {
                                    if (part.placementDefinition.pos.x > 0) {
                                        leftWheelGrounded = true;
                                    } else {
                                        rightWheelGrounded = true;
                                    }
                                }
                            } else if (part instanceof PartEngine) {
                                if (((PartEngine) part).currentGear == 0 && ((PartEngine) part).running) {
                                    foundNeutralEngine = true;
                                }
                            }
                        }
                        skidSteerActive = foundNeutralEngine && leftWheelGrounded && rightWheelGrounded;
                    }

                    //If skidSteer is active, do it now.
                    if (skidSteerActive) {
                        return steeringAngle / 20D;
                    }
                }

                //Steering force is initially is the value of the angle, divided by the distance to the wheels.
                //This means tighter turning for shorter-wheelbase vehicles and more input.
                //This is opposite of the torque-based forces for control surfaces.
                double turningForce = steeringAngle / turningDistance;
                //Decrease force by the speed of the vehicle.  If we are going fast, we can't turn as quickly.
                if (groundVelocity > 0.35D) {
                    turningForce *= Math.pow(0.3F, (groundVelocity * (1 - currentDownForce) - 0.35D));
                }
                //Calculate the force the steering produces.  Start with adjusting the steering factor by the ground velocity.
                //This is because the faster we go the quicker we need to turn to keep pace with the vehicle's movement.
                //We need to take speed-factor into account here, as that will make us move different lengths per tick.
                //Finally, we need to reduce this by a constant to get "proper" force..
                return turningForce * groundVelocity * (speedFactor / 0.35D) / 2D;
            }
        }
        return 0;
    }

    /**
     * Call this when moving vehicle to ensure they move correctly.
     * Failure to do this will result in things going badly!
     */
    private void moveVehicle() {
        if (towedByConnection == null || !towedByConnection.hitchConnection.mounted) {
            //First, update the vehicle ground device boxes.
            world.beginProfiling("GDBInit", true);
            collidedEntities.clear();
            groundDeviceCollective.updateCollisions();

            if (!definition.motorized.isAircraft) {
                //If we aren't on a road, try to find one.
                //Don't check for aircraft though.
                world.beginProfiling("RoadChecks", false);
                if ((frontFollower == null || rearFollower == null) && ticksExisted % 20 == 0) {
                    Point3D frontContact = groundDeviceCollective.getContactPoint(true);
                    Point3D rearContact = groundDeviceCollective.getContactPoint(false);
                    if (frontContact != null && rearContact != null) {
                        rearFollower = getFollower();
                        //If we are being towed, and we got followers, adjust them to our actual position.
                        //This is because we might have connected to the vehicle this tick, but won't be aligned
                        //to our towed position as connections are exact.
                        if (rearFollower != null) {
                            float pointDelta = (float) rearContact.distanceTo(frontContact);
                            if (towedByConnection == null) {
                                frontFollower = new RoadFollowingState(rearFollower, false).updateCurvePoints(pointDelta, LaneSelectionRequest.NONE);
                            } else if (towedByConnection.towingVehicle.lockedOnRoad) {
                                //Get delta between vehicle center and hitch, and vehicle center and hookup.  This gets total distance between vehicle centers.
                                //We need to use the GDB position for the towed, to the hitch, and same for ourselves.  Otherwise the value will be wrong.

                                //Get length between contact and hitch, in XZ plane.
                                //This has to be done in local coords to remove the Y-component.
                                //If the hitch is on the front, the towing vehicle is pushing us so we need to use front offsets.
                                boolean frontHitch = towedByConnection.towingEntity instanceof APart ? ((APart) towedByConnection.towingEntity).localOffset.z > 0 : towedByConnection.hitchConnection.pos.z > 0;
                                Point3D towingContact = towedByConnection.towingVehicle.groundDeviceCollective.getContactPoint(frontHitch);
                                Point3D towingHitchDelta = towedByConnection.hitchConnection.pos.copy().multiply(towedByConnection.towingEntity.scale);
                                if (towedByConnection.towingEntity instanceof APart) {
                                    APart part = (APart) towedByConnection.towingEntity;
                                    towingHitchDelta.rotate(part.localOrientation);
                                    towingHitchDelta.add(part.localOffset);
                                }
                                towingHitchDelta.subtract(towingContact);
                                towingHitchDelta.y = 0;

                                //Now get delta for ourselves, so we know how far to offset from this point.
                                //The same logic applies to us.
                                boolean frontHookup = towedByConnection.towedEntity instanceof APart ? ((APart) towedByConnection.towedEntity).localOffset.z > 0 : towedByConnection.hookupConnection.pos.z > 0;
                                Point3D towedContact = towedByConnection.towedVehicle.groundDeviceCollective.getContactPoint(frontHookup);
                                Point3D towedHitchDelta = towedByConnection.hookupConnection.pos.copy().multiply(towedByConnection.towedEntity.scale);
                                if (towedByConnection.towedEntity instanceof APart) {
                                    APart part = (APart) towedByConnection.towedEntity;
                                    towedHitchDelta.rotate(part.localOrientation);
                                    towedHitchDelta.add(part.localOffset);
                                }
                                towedHitchDelta.subtract(towedContact);
                                towedHitchDelta.y = 0;

                                //Now that we have both points, get the delta between them and set followers.
                                float segmentDelta = (float) (towingHitchDelta.length() + towedHitchDelta.length());
                                selectedSegment = ((AEntityVehicleD_Moving) towedByConnection.towingVehicle).selectedSegment;
                                if (frontHitch) {
                                    if (frontHookup) {
                                        invertedRoadOrientation = true;
                                        frontFollower = new RoadFollowingState(((AEntityVehicleD_Moving) towedByConnection.towingVehicle).frontFollower, invertedRoadOrientation);
                                        frontFollower.updateCurvePoints(-segmentDelta, selectedSegment);
                                        rearFollower = new RoadFollowingState(frontFollower, false);
                                        rearFollower.updateCurvePoints(-pointDelta, selectedSegment);
                                    } else {
                                        invertedRoadOrientation = false;
                                        rearFollower = new RoadFollowingState(((AEntityVehicleD_Moving) towedByConnection.towingVehicle).frontFollower, invertedRoadOrientation);
                                        rearFollower.updateCurvePoints(segmentDelta, selectedSegment);
                                        frontFollower = new RoadFollowingState(rearFollower, false);
                                        frontFollower.updateCurvePoints(pointDelta, selectedSegment);
                                    }
                                } else {
                                    if (frontHookup) {
                                        invertedRoadOrientation = false;
                                        frontFollower = new RoadFollowingState(((AEntityVehicleD_Moving) towedByConnection.towingVehicle).rearFollower, invertedRoadOrientation);
                                        frontFollower.updateCurvePoints(-segmentDelta, selectedSegment);
                                        rearFollower = new RoadFollowingState(frontFollower, false);
                                        rearFollower.updateCurvePoints(-pointDelta, selectedSegment);
                                    } else {
                                        invertedRoadOrientation = true;
                                        rearFollower = new RoadFollowingState(((AEntityVehicleD_Moving) towedByConnection.towingVehicle).rearFollower, invertedRoadOrientation);
                                        rearFollower.updateCurvePoints(segmentDelta, selectedSegment);
                                        frontFollower = new RoadFollowingState(rearFollower, false);
                                        frontFollower.updateCurvePoints(pointDelta, selectedSegment);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //If we are on a road, we need to bypass the logic for pitch/yaw/roll checks, and GDB checks.
            //This is because if we are on a road we need to follow the road's curve.
            //If we have both followers, do road-following logic.
            //If we don't, or we're turning off the road, do normal vehicle logic.
            if (frontFollower != null && rearFollower != null) {
                world.beginProfiling("RoadOperations", false);

                //Check for the potential to change the requested segment.
                //We can only do this if both our followers are on the same segment.
                LaneSelectionRequest requestedSegment;
                if (isVariableActive(LEFTTURNLIGHT_VARIABLE) == isVariableActive(RIGHTTURNLIGHT_VARIABLE)) {
                    requestedSegment = LaneSelectionRequest.NONE;
                } else if (isVariableActive(LEFTTURNLIGHT_VARIABLE)) {
                    requestedSegment = goingInReverse ? LaneSelectionRequest.RIGHT : LaneSelectionRequest.LEFT;
                } else {
                    requestedSegment = goingInReverse ? LaneSelectionRequest.LEFT : LaneSelectionRequest.RIGHT;
                }
                if (frontFollower.equals(rearFollower)) {
                    selectedSegment = requestedSegment;
                }

                float segmentDelta = (float) (totalPathDelta - prevTotalPathDelta);
                prevTotalPathDelta = totalPathDelta;
                frontFollower = frontFollower.updateCurvePoints(segmentDelta, selectedSegment);
                rearFollower = rearFollower.updateCurvePoints(segmentDelta, selectedSegment);
                Point3D rearPoint = groundDeviceCollective.getContactPoint(false);

                //Check to make sure followers are still valid, and do logic.
                if (frontFollower != null && rearFollower != null && rearPoint != null) {
                    //Set our position so we're aligned with the road.
                    //To do this, we get the distance between our contact points for front and rear, and then interpolate between them.
                    //First get the rear point.  This defines the delta for the movement of the vehicle.
                    rearPoint.rotate(orientation).add(position);
                    Point3D rearDesiredPoint = rearFollower.getCurrentPoint();

                    //Apply the motion based on the delta between the actual and desired.
                    //Also set motion Y to 0 in case we were doing ground device things.
                    roadMotion.set(rearDesiredPoint);
                    roadMotion.subtract(rearPoint);
                    if (roadMotion.length() > 1) {
                        roadMotion.set(0, 0, 0);
                        frontFollower = null;
                        rearFollower = null;
                    } else {
                        motion.y = 0;

                        //Now get the front desired point.  We don't care about actual point here, as we set angle based on the point delta.
                        //Desired angle is the one that gives us the vector between the front and rear points.
                        Point3D desiredVector = frontFollower.getCurrentPoint().subtract(rearDesiredPoint);
                        double yawDelta = Math.toDegrees(Math.atan2(desiredVector.x, desiredVector.z));
                        double pitchDelta = -Math.toDegrees(Math.atan2(desiredVector.y, Math.hypot(desiredVector.x, desiredVector.z)));
                        double rollDelta = rearFollower.getCurrentRoll();
                        if (definition.motorized.maxTiltAngle != 0) {
                            rollDelta -= definition.motorized.maxTiltAngle * 2.0 * Math.min(0.5, velocity / 2D) * getSteeringAngle();
                        }
                        roadRotation.set(pitchDelta - orientation.angles.x, yawDelta, rollDelta - orientation.angles.z);
                        roadRotation.y = roadRotation.getClampedYDelta(orientation.angles.y);
                        if (!world.isClient()) {
                            if (towedByConnection != null) {
                                addToSteeringAngle(towedByConnection.towingVehicle.getSteeringAngle() - getSteeringAngle());
                            } else {
                                addToSteeringAngle((goingInReverse ? -roadRotation.y : roadRotation.y) * 1.5D);
                            }
                        }
                    }
                } else {
                    //Set followers to null, as something is invalid.
                    frontFollower = null;
                    rearFollower = null;
                }
            }

            groundMotion.set(0, 0, 0);
            boolean fallingDown = motion.y < 0;
            lockedOnRoad = frontFollower != null && rearFollower != null;
            //If followers aren't valid, do normal logic.
            if (!lockedOnRoad) {
                //If any ground devices are collided after our movement, apply corrections to prevent this.
                //The first correction we apply is +y motion.  This counteracts gravity, and any GDBs that may
                //have been moved into the ground by the application of our motion and rotation.  We do this before collision
                //boxes, as we don't want gravity to cause us to move into something when we really shouldn't move down because
                //all the GDBs prevent this.  In either case, apply +y motion to get all the GDBs out of the ground.
                //This may not be possible, however, if the boxes are too deep into the ground.  We don't want vehicles to
                //instantly climb mountains.  Because of this, we add only 1/8 block, or enough motionY to prevent collision,
                //whichever is the lower of the two.  If we apply boost, update our collision boxes before the next step.
                //Note that this logic is not applied on trailers, as they use special checks with only rotations for movement.
                world.beginProfiling("GroundBoostCheck", false);
                groundMotion.y = groundDeviceCollective.getMaxCollisionDepth() / speedFactor;
                if (groundMotion.y > 0) {
                    world.beginProfiling("GroundBoostApply", false);
                    //Make sure boost doesn't exceed the config value.
                    groundMotion.y = Math.min(groundMotion.y, ConfigSystem.settings.general.climbSpeed.value / speedFactor);

                    //If adding our boost would make motion.y positive, set motion.y to zero and apply the remaining boost.
                    //This is done as it's clear motion.y is just moving the vehicle into the ground.
                    //Having negative motion.y is okay if we don't boost above, as this just means we are falling to the ground via gravity.
                    //In this case, we use our boost, but set it to 0 as we want to just attenuate the negative motion, not remove it.
                    if (motion.y <= 0 && motion.y + groundMotion.y > 0) {
                        groundMotion.y += motion.y;
                        motion.y = 0;
                    } else {
                        motion.y += groundMotion.y;
                        groundMotion.y = 0;
                    }
                    groundDeviceCollective.updateCollisions();
                }

                //After checking the ground devices to ensure we aren't shoving ourselves into the ground, we try to move the vehicle.
                //If the vehicle can move without a collision box colliding with something, then we can move to the re-positioning of the vehicle.
                //If we hit something, however, we need to inhibit the movement so we don't do that.
                //This prevents vehicles from phasing through walls even though they are driving on the ground.
                //If we are being towed, apply this movement to the towing vehicle, not ourselves, as this can lead to the vehicle getting stuck.
                world.beginProfiling("CollisionCheck_" + allBlockCollisionBoxes.size(), false);
                if (isCollisionBoxCollided()) {
                    world.beginProfiling("CollisionHandling", false);
                    if (towedByConnection != null) {
                        Point3D initalMotion = motion.copy();
                        if (correctCollidingMovement()) {
                            return;
                        }
                        towedByConnection.towingVehicle.motion.add(motion).subtract(initalMotion);
                    } else if (correctCollidingMovement()) {
                        return;
                    }
                    groundDeviceCollective.updateCollisions();
                }
                if (fallingDown || towedByConnection != null) {
                    world.beginProfiling("GroundHandlingPitch", false);
                    groundDeviceCollective.performPitchCorrection(groundMotion);
                    //Don't do roll correction if we don't have roll.
                    if (groundDeviceCollective.canDoRollChecks()) {
                        world.beginProfiling("GroundHandlingRoll", false);
                        groundDeviceCollective.performRollCorrection(groundMotion);
                    }

                    //If we are flagged as a tilting vehicle try to keep us upright, unless we are turning, in which case turn into the turn.
                    if (definition.motorized.maxTiltAngle != 0) {
                        rotation.angles.z = -orientation.angles.z - definition.motorized.maxTiltAngle * 2.0 * Math.min(0.5, velocity / 2D) * getSteeringAngle();
                    }
                }
            }

            //If we collided with any entities, move us with them.
            //This allows for transports without mounting.
            if (!collidedEntities.isEmpty()) {
                world.beginProfiling("EntityMoveAlong", false);
                for (AEntityE_Interactable<?> interactable : collidedEntities) {
                    //Set angluar movement delta.
                    if (interactable instanceof AEntityVehicleD_Moving) {
                        vehicleCollisionRotation.set(interactable.orientation).multiplyTranspose(interactable.prevOrientation);
                        vehicleCollisionRotation.convertToAngles();
                    }

                    //Get vector from collided box to this entity.
                    Point3D centerOffset = position.copy().subtract(interactable.prevPosition);

                    //Add rotation contribution to offset.
                    vehicleCollisionMotion.set(centerOffset);
                    vehicleCollisionMotion.rotate(vehicleCollisionRotation);
                    vehicleCollisionMotion.subtract(centerOffset);

                    //Add linear contribution to offset.
                    vehicleCollisionMotion.add(interactable.position).subtract(interactable.prevPosition);

                    //If we just contacted an entity, adjust our motion to match that entity's motion.
                    //We take our motion, and then remove it so it's the delta to that entity.
                    //This ensures that if we're moving and land on an entity, we don't run off.
                    if (lastCollidedEntity == null) {
                        lastCollidedEntity = interactable;
                        motion.subtract(lastCollidedEntity.motion);
                    }

                    //Only check one for now.  We could do multiple, but then we'd have to do maths.
                    break;
                }
            } else {
                if (lastCollidedEntity != null) {
                    //Add-back to our motion by adding the entity's motion.
                    motion.add(lastCollidedEntity.motion);
                    lastCollidedEntity = null;
                }
            }

            //Now that that the movement has been checked, move the vehicle.
            world.beginProfiling("ApplyMotions", false);
            motionApplied.set(motion).scale(speedFactor).add(groundMotion);
            rotationApplied.angles.set(rotation.angles);

            //Add road contributions.
            if (lockedOnRoad) {
                motionApplied.add(roadMotion);
                rotationApplied.angles.add(roadRotation);
                if (towedByConnection != null) {
                    pathingApplied = ((AEntityVehicleD_Moving) towedByConnection.towingVehicle).pathingApplied;
                    if (invertedRoadOrientation) {
                        pathingApplied = -pathingApplied;
                    }
                } else {
                    pathingApplied = goingInReverse ? -velocity * speedFactor : velocity * speedFactor;
                }
            } else {
                pathingApplied = 0;
            }

            //Add colliding vehicle contributions.
            if (lastCollidedEntity != null) {
                motionApplied.add(vehicleCollisionMotion);
                rotationApplied.angles.add(vehicleCollisionRotation.angles);
            }

            //All contributions done, add calculated motions.
            position.add(motionApplied);
            if (!rotationApplied.angles.isZero()) {
                rotationApplied.updateToAngles();
                orientation.multiply(rotationApplied).convertToAngles();
            }
            totalPathDelta += pathingApplied;

            //Now adjust our movement to sync with the server.
            if (world.isClient()) {
                //Get the delta difference, and square it.  Then divide it by 25.
                //This gives us a good "rubberbanding correction" formula for deltas.
                //We add this correction motion to the existing motion applied.
                //We need to keep the sign after squaring, however, as that tells us what direction to apply the deltas in.
                clientDeltaM.add(motionApplied);
                clientDeltaMApplied.set(serverDeltaM).subtract(clientDeltaM);
                if (!clientDeltaMApplied.isZero()) {
                    clientDeltaMApplied.x *= Math.abs(clientDeltaMApplied.x);
                    clientDeltaMApplied.y *= Math.abs(clientDeltaMApplied.y);
                    clientDeltaMApplied.z *= Math.abs(clientDeltaMApplied.z);
                    clientDeltaMApplied.scale(1D / 25D);
                    clientDeltaM.add(clientDeltaMApplied);
                    position.add(clientDeltaMApplied);
                }

                //Note that orientation wasn't a direct angle-addition, as the rotation is applied relative to the current
                //orientation.  This means that if we yaw 5 degrees, but are rolling 10 degrees, then we need to not do the
                //yaw rotation in the XZ plane, but instead in that relative-rotated plane.
                //To account for this, we get the angle delta between the prior and current orientation, and use that for the delta. 
                //Though before we do this we check if those angles were non-zero, as no need to do math if they are.
                if (!rotationApplied.angles.isZero()) {
                    rotationApplied.angles.set(orientation.angles).subtract(prevOrientation.angles).clamp180();
                    clientDeltaR.add(rotationApplied.angles);
                }
                clientDeltaRApplied.set(serverDeltaR).subtract(clientDeltaR);
                if (!clientDeltaRApplied.isZero()) {
                    clientDeltaRApplied.x *= Math.abs(clientDeltaRApplied.x);
                    clientDeltaRApplied.y *= Math.abs(clientDeltaRApplied.y);
                    clientDeltaRApplied.z *= Math.abs(clientDeltaRApplied.z);
                    clientDeltaRApplied.scale(1D / 25D);
                    if (clientDeltaRApplied.x < -5)
                        clientDeltaRApplied.x = -5;
                    if (clientDeltaRApplied.x > 5)
                        clientDeltaRApplied.x = 5;
                    if (clientDeltaRApplied.y < -5)
                        clientDeltaRApplied.y = -5;
                    if (clientDeltaRApplied.y > 5)
                        clientDeltaRApplied.y = 5;
                    if (clientDeltaRApplied.z < -5)
                        clientDeltaRApplied.z = -5;
                    if (clientDeltaRApplied.z > 5)
                        clientDeltaRApplied.z = 5;
                    clientDeltaR.add(clientDeltaRApplied);
                    orientation.angles.add(clientDeltaRApplied);
                    orientation.updateToAngles();
                }

                clientDeltaP += pathingApplied;
                clientDeltaPApplied = serverDeltaP - clientDeltaP;
                if (clientDeltaPApplied != 0) {
                    clientDeltaPApplied *= Math.abs(clientDeltaPApplied);
                    clientDeltaPApplied *= 1D / 25D;
                    clientDeltaP += clientDeltaPApplied;
                    totalPathDelta += clientDeltaPApplied;
                }
            } else {
                addToServerDeltas(null, null, 0);
            }
        } else {
            //Mounted vehicles don't do most motions, only a sub-set of them.
            world.beginProfiling("ApplyMotions", true);
            motionApplied.set(motion).scale(speedFactor);
            position.add(motionApplied);

            //Rotation for mounted connections aligns using orientation, not angle-deltas.
            orientation.set(rotation).convertToAngles();

            //For syncing, just add our deltas.  We don't actually do syncing operations here.
            AEntityVehicleD_Moving towingVehicle = towedByConnection.towingVehicle;
            if (world.isClient()) {
                clientDeltaM.add(towingVehicle.clientDeltaMApplied);
                clientDeltaR.add(towingVehicle.clientDeltaRApplied);
                clientDeltaP += towingVehicle.clientDeltaPApplied;
            } else {
                serverDeltaM.add(towingVehicle.serverDeltaMApplied);
                serverDeltaR.add(towingVehicle.serverDeltaRApplied);
                serverDeltaP += towingVehicle.serverDeltaPApplied;
            }
        }
        world.endProfiling();
    }

    /**
     * Checks if we have a collided collision box.  If so, true is returned.
     */
    private boolean isCollisionBoxCollided() {
        if (motion.length() > 0.001) {
            boolean clearedCache = false;
            for (BoundingBox box : allBlockCollisionBoxes) {
                tempBoxPosition.set(box.globalCenter).subtract(position).rotate(rotation).subtract(box.globalCenter).add(position).addScaled(motion, speedFactor);
                if (!box.collidesWithLiquids && world.checkForCollisions(box, tempBoxPosition, !clearedCache)) {
                    return true;
                }
                clearedCache = true;
            }
        }
        return false;
    }

    /**
     * If a collision box collided, we need to restrict our proposed movement.
     * Do this by removing motions that cause collisions.
     * If the motion has a value of 0, skip it as it couldn't have caused the collision.
     * Note that even though motionY may have been adjusted for ground device operation prior to this call,
     * we shouldn't have an issue with the change as this logic takes priority over that logic to ensure
     * no collision box collides with another block, even if it requires all the ground devices to be collided.
     * If true is returned here, it means this vehicle was destroyed in a collision, and no further processing should
     * be done on it as states may be un-defined.
     */
    private boolean correctCollidingMovement() {
        //First check the X-axis.
        if (motion.x != 0) {
            for (BoundingBox box : allBlockCollisionBoxes) {
                double collisionDepth = getCollisionForAxis(box, true, false, false);
                if (collisionDepth == -1) {
                    return true;
                } else if (collisionDepth == -2) {
                    break;
                } else {
                    if (motion.x > 0) {
                        motion.x = Math.max(motion.x - collisionDepth / speedFactor, 0);
                    } else if (motion.x < 0) {
                        motion.x = Math.min(motion.x + collisionDepth / speedFactor, 0);
                    }
                }
            }
        }

        //Do the same for the Z-axis
        if (motion.z != 0) {
            for (BoundingBox box : allBlockCollisionBoxes) {
                double collisionDepth = getCollisionForAxis(box, false, false, true);
                if (collisionDepth == -1) {
                    return true;
                } else if (collisionDepth == -2) {
                    break;
                } else {
                    if (motion.z > 0) {
                        motion.z = Math.max(motion.z - collisionDepth / speedFactor, 0);
                    } else if (motion.z < 0) {
                        motion.z = Math.min(motion.z + collisionDepth / speedFactor, 0);
                    }
                }
            }
        }

        //Now that the XZ motion has been limited based on collision we can move in the Y.
        if (motion.y != 0) {
            for (BoundingBox box : allBlockCollisionBoxes) {
                double collisionDepth = getCollisionForAxis(box, false, true, false);
                if (collisionDepth == -1) {
                    return true;
                } else if (collisionDepth == -2) {
                    break;
                } else if (collisionDepth != 0) {
                    if (motion.y > 0) {
                        motion.y = Math.max(motion.y - collisionDepth / speedFactor, 0);
                    } else if (motion.y < 0) {
                        motion.y = Math.min(motion.y + collisionDepth / speedFactor, 0);
                    }
                }
            }
        }

        //Check the rotation.
        if (!rotation.angles.isZero()) {
            for (BoundingBox box : allBlockCollisionBoxes) {
                tempBoxPosition.set(box.globalCenter).subtract(position).rotate(rotation).add(position).addScaled(motion, speedFactor);
                if (box.updateCollidingBlocks(world, tempBoxPosition.subtract(box.globalCenter))) {
                    rotation.setToZero();
                    break;
                }
            }
        }
        return false;
    }

    public void addToServerDeltas(Point3D motionAdded, Point3D rotationAdded, double pathingAdded) {
        if (rotationAdded != null) {
            //Packet call from server, add directly.
            serverDeltaM.add(motionAdded);
            serverDeltaR.add(rotationAdded);
            serverDeltaP += pathingAdded;
        } else {
            //Internal call, add normally and send packet if needed.
            if (!motionApplied.isZero()) {
                serverDeltaMApplied.set(motionApplied);
                serverDeltaM.add(motionApplied);
                if (!orientation.angles.equals(prevOrientation.angles)) {
                    rotationApplied.angles.set(orientation.angles).subtract(prevOrientation.angles).clamp180();
                    serverDeltaRApplied.set(rotationApplied.angles);
                    serverDeltaR.add(rotationApplied.angles);
                }
                serverDeltaPApplied += pathingApplied;
                serverDeltaP += pathingApplied;
                InterfaceManager.packetInterface.sendToAllClients(new PacketVehicleServerMovement((EntityVehicleF_Physics) this, motionApplied, rotationApplied.angles, pathingApplied));
            }
        }
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
    protected abstract void addToSteeringAngle(double degrees);

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
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setPoint3d("serverDeltaM", serverDeltaM);
        data.setPoint3d("serverDeltaR", serverDeltaR);
        data.setDouble("serverDeltaP", serverDeltaP);
        return data;
    }
}
