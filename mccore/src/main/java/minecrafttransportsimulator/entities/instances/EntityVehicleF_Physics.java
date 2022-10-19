package minecrafttransportsimulator.entities.instances;

import java.util.HashSet;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TowingConnection;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.jsondefs.JSONVariableModifier;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * This class adds the final layer of physics calculations on top of the
 * existing entity calculations.  Various control surfaces are present, as
 * well as helper functions and logic for controlling those surfaces.
 * Note that angle variables here should be divided by 10 to get actual angle.
 *
 * @author don_bruce
 */
public class EntityVehicleF_Physics extends AEntityVehicleE_Powered {
    //Aileron.
    @DerivedValue
    public double aileronInput;
    @DerivedValue
    public double aileronAngle;
    @DerivedValue
    public double aileronTrim;
    public static final double MAX_AILERON_ANGLE = 25;
    public static final double MAX_AILERON_TRIM = 10;
    public static final double AILERON_DAMPEN_RATE = 0.6;
    public static final String AILERON_INPUT_VARIABLE = "input_aileron";
    public static final String AILERON_VARIABLE = "aileron";
    public static final String AILERON_TRIM_VARIABLE = "trim_aileron";

    //Elevator.
    @DerivedValue
    public double elevatorInput;
    @DerivedValue
    public double elevatorAngle;
    @DerivedValue
    public double elevatorTrim;
    public static final double MAX_ELEVATOR_ANGLE = 25;
    public static final double MAX_ELEVATOR_TRIM = 10;
    public static final double ELEVATOR_DAMPEN_RATE = 0.6;
    public static final String ELEVATOR_INPUT_VARIABLE = "input_elevator";
    public static final String ELEVATOR_VARIABLE = "elevator";
    public static final String ELEVATOR_TRIM_VARIABLE = "trim_elevator";

    //Rudder.
    @DerivedValue
    public double rudderInput;
    @DerivedValue
    public double rudderAngle;
    @DerivedValue
    public double rudderTrim;
    public static final double MAX_RUDDER_ANGLE = 45;
    public static final double MAX_RUDDER_TRIM = 10;
    public static final double RUDDER_DAMPEN_RATE = 2.0;
    public static final String RUDDER_INPUT_VARIABLE = "input_rudder";
    public static final String RUDDER_VARIABLE = "rudder";
    public static final String RUDDER_TRIM_VARIABLE = "trim_rudder";

    //Flaps.
    public static final short MAX_FLAP_ANGLE_REFERENCE = 350;
    @DerivedValue
    public double flapDesiredAngle;
    public double flapCurrentAngle;
    public static final String FLAPS_VARIABLE = "flaps_setpoint";

    //External state control.
    public boolean turningLeft;
    public boolean turningRight;
    public byte turningCooldown;
    @DerivedValue
    public double autopilotSetting;
    public double airDensity;
    public static final String AUTOPILOT_VARIABLE = "autopilot";
    public static final String AUTOLEVEL_VARIABLE = "auto_level";
    public int controllerCount;
    public IWrapperPlayer lastController;

    //Internal states.
    private boolean hasRotors;
    private double trackAngle;
    private final Point3D normalizedVelocityVector = new Point3D();
    private final Point3D verticalVector = new Point3D();
    private final Point3D sideVector = new Point3D();
    private final Point3D hitchPrevOffset = new Point3D();
    private final Point3D hitchCurrentOffset = new Point3D();
    private final Set<AEntityG_Towable<?>> towedEntitiesCheckedForWeights = new HashSet<>();

    //Properties.
    @ModifiedValue
    public float currentWingArea;
    @ModifiedValue
    public float currentWingSpan;
    @ModifiedValue
    public float currentAileronArea;
    @ModifiedValue
    public float currentElevatorArea;
    @ModifiedValue
    public float currentRudderArea;
    @ModifiedValue
    public float currentDragCoefficient;
    @ModifiedValue
    public float currentBallastVolume;
    @ModifiedValue
    public float currentAxleRatio;

    //Coefficients.
    private double wingLiftCoeff;
    private double aileronLiftCoeff;
    private double elevatorLiftCoeff;
    private double rudderLiftCoeff;
    private double dragCoeff;

    //Forces.
    private double dragForce;//kg*m/ticks^2
    private double wingForce;//kg*m/ticks^2
    private double aileronForce;//kg*m/ticks^2
    private double elevatorForce;//kg*m/ticks^2
    private double rudderForce;//kg*m/ticks^2
    private double ballastForce;//kg*m/ticks^2
    private double gravitationalForce;//kg*m/ticks^2
    private final Point3D thrustForce = new Point3D();//kg*m/ticks^2
    private double thrustForceValue;
    private final Point3D towingThrustForce = new Point3D();//kg*m/ticks^2
    private final Point3D totalForce = new Point3D();//kg*m/ticks^2

    //Torques.
    private double momentRoll;//kg*m^2
    private double momentPitch;//kg*m^2
    private double momentYaw;//kg*m^2
    private double aileronTorque;//kg*m^2/ticks^2
    private double elevatorTorque;//kg*m^2/ticks^2
    private double rudderTorque;//kg*m^2/ticks^2
    private final Point3D thrustTorque = new Point3D();//kg*m^2/ticks^2
    private final Point3D totalTorque = new Point3D();//kg*m^2/ticks^2
    private final Point3D rotorRotation = new Point3D();//degrees

    public EntityVehicleF_Physics(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, placingPlayer, data);
        this.flapCurrentAngle = data.getDouble("flapCurrentAngle");
    }

    @Override
    public void update() {
        super.update();
        world.beginProfiling("VehicleF_Level", true);
        //Set vectors.
        verticalVector.set(0D, 1D, 0D).rotate(orientation);
        normalizedVelocityVector.set(motion).normalize();
        sideVector.set(verticalVector.crossProduct(headingVector));

        //Parse out variables.
        aileronInput = getVariable(AILERON_INPUT_VARIABLE);
        aileronTrim = getVariable(AILERON_TRIM_VARIABLE);
        elevatorInput = getVariable(ELEVATOR_INPUT_VARIABLE);
        elevatorTrim = getVariable(ELEVATOR_TRIM_VARIABLE);
        rudderInput = getVariable(RUDDER_INPUT_VARIABLE);
        rudderTrim = getVariable(RUDDER_TRIM_VARIABLE);
        autopilotSetting = getVariable(AUTOPILOT_VARIABLE);
        flapDesiredAngle = getVariable(FLAPS_VARIABLE);

        //Adjust flaps to current setting.
        if (definition.motorized.flapNotches != null && !definition.motorized.flapNotches.isEmpty()) {
            if (flapCurrentAngle < flapDesiredAngle) {
                flapCurrentAngle += definition.motorized.flapSpeed;
            } else if (flapCurrentAngle > flapDesiredAngle) {
                flapCurrentAngle -= definition.motorized.flapSpeed;
            }
            if (Math.abs(flapCurrentAngle - flapDesiredAngle) < definition.motorized.flapSpeed) {
                flapCurrentAngle = flapDesiredAngle;
            }
        }
        world.endProfiling();
    }

    @Override
    public boolean requiresDeltaUpdates() {
        return true;
    }

    @Override
    public double getMass() {
        //Need to use a list here to make sure we don't end up with infinite recursion due to bad trailer linkings.
        //This could lock up a world if not detected!
        double combinedMass = super.getMass();
        if (!towingConnections.isEmpty()) {
            AEntityG_Towable<?> towedEntity;
            for (TowingConnection connection : towingConnections) {
                //Only check once per base entity.
                towedEntity = connection.towedVehicle;
                if (towedEntitiesCheckedForWeights.contains(towedEntity)) {
                    InterfaceManager.coreInterface.logError("Infinite loop detected on weight checking code!  Is a trailer towing the thing that's towing it?");
                    break;
                } else {
                    towedEntitiesCheckedForWeights.add(towedEntity);
                    combinedMass += towedEntity.getMass();
                    towedEntitiesCheckedForWeights.clear();
                }
            }
        }
        return combinedMass;
    }

    @Override
    protected double getSteeringAngle() {
        return -rudderInput / (float) MAX_RUDDER_ANGLE;
    }

    @Override
    protected void addToSteeringAngle(double degrees) {
        //Invert the degrees, as rudder is inverted from normal steering.
        double delta;
        if (rudderInput - degrees > MAX_RUDDER_ANGLE) {
            delta = MAX_RUDDER_ANGLE - rudderInput;
        } else if (rudderInput - degrees < -MAX_RUDDER_ANGLE) {
            delta = -MAX_RUDDER_ANGLE - rudderInput;
        } else {
            delta = -degrees;
        }
        setVariable(RUDDER_INPUT_VARIABLE, rudderInput + delta);
        InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, RUDDER_INPUT_VARIABLE, delta));
    }

    @Override
    protected void updateVariableModifiers() {
        currentWingArea = (float) (definition.motorized.wingArea + definition.motorized.wingArea * 0.15F * flapCurrentAngle / MAX_FLAP_ANGLE_REFERENCE);
        currentWingSpan = definition.motorized.wingSpan;
        currentAileronArea = definition.motorized.aileronArea;
        currentElevatorArea = definition.motorized.elevatorArea;
        currentRudderArea = definition.motorized.rudderArea;
        currentDragCoefficient = definition.motorized.dragCoefficient;
        currentBallastVolume = definition.motorized.ballastVolume;
        currentDownForce = definition.motorized.downForce;
        currentBrakingFactor = definition.motorized.brakingFactor;
        currentOverSteer = definition.motorized.overSteer;
        currentUnderSteer = definition.motorized.underSteer;
        currentAxleRatio = definition.motorized.axleRatio;
        aileronAngle = aileronInput;
        setVariable(AILERON_VARIABLE, aileronAngle);
        elevatorAngle = elevatorInput;
        setVariable(ELEVATOR_VARIABLE, elevatorAngle);
        rudderAngle = rudderInput;
        setVariable(RUDDER_VARIABLE, rudderAngle);

        //Adjust current variables to modifiers, if any exist.
        if (definition.variableModifiers != null) {
            for (JSONVariableModifier modifier : definition.variableModifiers) {
                switch (modifier.variable) {
                    case "wingArea":
                        currentWingArea = adjustVariable(modifier, currentWingArea);
                        break;
                    case "wingSpan":
                        currentWingSpan = adjustVariable(modifier, currentWingSpan);
                        break;
                    case "aileronArea":
                        currentAileronArea = adjustVariable(modifier, currentAileronArea);
                        break;
                    case "elevatorArea":
                        currentElevatorArea = adjustVariable(modifier, currentElevatorArea);
                        break;
                    case "rudderArea":
                        currentRudderArea = adjustVariable(modifier, currentRudderArea);
                        break;
                    case "dragCoefficient":
                        currentDragCoefficient = adjustVariable(modifier, currentDragCoefficient);
                        break;
                    case "ballastVolume":
                        currentBallastVolume = adjustVariable(modifier, currentBallastVolume);
                        break;
                    case "downForce":
                        currentDownForce = adjustVariable(modifier, currentDownForce);
                        break;
                    case "brakingFactor":
                        currentBrakingFactor = adjustVariable(modifier, currentBrakingFactor);
                        break;
                    case "overSteer":
                        currentOverSteer = adjustVariable(modifier, currentOverSteer);
                        break;
                    case "underSteer":
                        currentUnderSteer = adjustVariable(modifier, currentUnderSteer);
                        break;
                    case "axleRatio":
                        currentAxleRatio = adjustVariable(modifier, currentAxleRatio);
                        break;
                    case AILERON_VARIABLE:
                        aileronAngle = adjustVariable(modifier, (float) aileronAngle);
                        setVariable(AILERON_VARIABLE, aileronAngle);
                        break;
                    case ELEVATOR_VARIABLE:
                        elevatorAngle = adjustVariable(modifier, (float) elevatorAngle);
                        setVariable(ELEVATOR_VARIABLE, elevatorAngle);
                        break;
                    case RUDDER_VARIABLE:
                        rudderAngle = adjustVariable(modifier, (float) rudderAngle);
                        setVariable(RUDDER_VARIABLE, rudderAngle);
                        break;
                    default:
                        setVariable(modifier.variable, adjustVariable(modifier, (float) getVariable(modifier.variable)));
                        break;
                }
            }
        }
    }

    @Override
    protected void getForcesAndMotions() {
        //Get engine thrust force contributions.  This happens for all vehicles, towed or not.
        hasRotors = false;
        thrustForce.set(0, 0, 0);
        thrustTorque.set(0, 0, 0);
        rotorRotation.set(0, 0, 0);
        thrustForceValue = 0;
        for (APart part : allParts) {
            if (part instanceof PartEngine) {
                thrustForceValue += ((PartEngine) part).addToForceOutput(thrustForce, thrustTorque);
            } else if (part instanceof PartPropeller) {
                PartPropeller propeller = (PartPropeller) part;
                thrustForceValue += propeller.addToForceOutput(thrustForce, thrustTorque);
                if (propeller.definition.propeller.isRotor && groundDeviceCollective.isAnythingOnGround()) {
                    hasRotors = true;
                    if (getVariable(AUTOLEVEL_VARIABLE) != 0) {
                        rotorRotation.set((-(elevatorAngle + elevatorTrim) - orientation.angles.x) / MAX_ELEVATOR_ANGLE, -5D * rudderAngle / MAX_RUDDER_ANGLE, ((aileronAngle + aileronTrim) - orientation.angles.z) / MAX_AILERON_ANGLE);
                    } else {
                        if (autopilotSetting == 0) {
                            rotorRotation.set(-5D * elevatorAngle / MAX_ELEVATOR_ANGLE, -5D * rudderAngle / MAX_RUDDER_ANGLE, 5D * aileronAngle / MAX_AILERON_ANGLE);
                        } else {
                            if (orientation.angles.x < -1) {
                                rotorRotation.x = 1;
                            } else if (orientation.angles.x > 1) {
                                rotorRotation.x = -1;
                            } else {
                                rotorRotation.x = -orientation.angles.x;
                            }
                            if (orientation.angles.z < -1) {
                                rotorRotation.z = 1;
                            } else if (orientation.angles.z > 1) {
                                rotorRotation.z = -1;
                            } else {
                                rotorRotation.z = -orientation.angles.z;
                            }
                            rotorRotation.y = -5D * rudderAngle / MAX_RUDDER_ANGLE;
                        }
                    }
                }
            }
        }

        //If we are free, do normal updates.  But if we are towed by a vehicle, do trailer forces instead.
        //This prevents trailers from behaving badly and flinging themselves into the abyss.
        if (towedByConnection == null) {
            //Set moments and air density.
            airDensity = 1.225 * Math.pow(2, -position.y / (500D * world.getMaxHeight() / 256D));
            momentRoll = definition.motorized.emptyMass * (1.5F + fuelTank.getFluidLevel() / 10000F);
            momentPitch = 2D * currentMass;
            momentYaw = 3D * currentMass;

            //If we are towing any non-mounted vehicles, get their thrust contributions as well.
            double towedThrust = getRecursiveTowingThrust();
            if (towedThrust != 0) {
                towingThrustForce.set(0, 0, towedThrust).rotate(orientation);
                thrustForce.add(towingThrustForce);
            }

            //Get forces.  Some forces are specific to JSON sections.
            //First get gravity.
            gravitationalForce = currentBallastVolume == 0 ? currentMass * (9.8 / 400) : 0;
            if (!definition.motorized.isAircraft) {
                gravitationalForce *= ConfigSystem.settings.general.gravityFactor.value;
            }

            //Get the track angle.  This is used for control surfaces.
            trackAngle = -Math.toDegrees(Math.asin(verticalVector.dotProduct(normalizedVelocityVector, true)));

            //Set blimp-specific states before calculating forces.
            if (definition.motorized.isBlimp) {
                //Blimps are turned with rudders, not ailerons.  This puts the keys at an odd location.  To compensate, 
                //we set the rudder to the aileron if the aileron is greater or less than the rudder.  That way no matter 
                //which key is pressed, they both activate the rudder for turning.
                if ((aileronAngle < 0 && aileronAngle < rudderAngle) || (aileronAngle > 0 && aileronAngle > rudderAngle)) {
                    rudderAngle = aileronAngle;
                }

                //If we have the brake pressed at a slow speed, stop the blimp.
                //This is needed to prevent runaway blimps.
                if (Math.abs(velocity) < 0.15 && (brake > 0 || parkingBrakeOn)) {
                    motion.x = 0;
                    motion.z = 0;
                    thrustForce.set(0D, 0D, 0D);
                    thrustTorque.set(0D, 0D, 0D);
                }
            }

            //Get the lift coefficients and states for control surfaces.
            double yawAngleDelta = Math.toDegrees(Math.asin(sideVector.dotProduct(normalizedVelocityVector, true)));
            wingLiftCoeff = getLiftCoeff(trackAngle, 2 + flapCurrentAngle / MAX_FLAP_ANGLE_REFERENCE);
            aileronLiftCoeff = getLiftCoeff((aileronAngle + aileronTrim), 2);
            elevatorLiftCoeff = getLiftCoeff(-2.5 + trackAngle - (elevatorAngle + elevatorTrim), 2);
            rudderLiftCoeff = getLiftCoeff(yawAngleDelta - (rudderAngle + rudderTrim), 2);

            //Get the drag coefficient and force.
            if (definition.motorized.isBlimp) {
                dragCoeff = 0.004F * Math.pow(Math.abs(yawAngleDelta), 2) + currentDragCoefficient;
            } else if (definition.motorized.isAircraft) {
                //Aircraft are 0.03 by default, or whatever is specified.
                dragCoeff = 0.0004F * Math.pow(trackAngle, 2) + currentDragCoefficient;
            } else {
                dragCoeff = currentDragCoefficient;
                //If we aren't an aircraft, check for grounded ground devices.
                //If we don't have any grounded ground devices, assume we are in the air or in water.
                //This results in an increase in drag due to poor airflow.
                if (groundDeviceCollective.groundedGroundDevices.isEmpty()) {
                    dragCoeff *= 3D;
                }
            }
            if (definition.motorized.crossSectionalArea > 0) {
                dragForce = 0.5F * airDensity * velocity * velocity * definition.motorized.crossSectionalArea * dragCoeff;
            } else if (currentWingSpan > 0) {
                dragForce = 0.5F * airDensity * velocity * velocity * currentWingArea * (dragCoeff + wingLiftCoeff * wingLiftCoeff / (Math.PI * definition.motorized.wingSpan * definition.motorized.wingSpan / currentWingArea * 0.8));
            } else {
                dragForce = 0.5F * airDensity * velocity * velocity * 5.0F * dragCoeff;
            }

            //Get ballast force.
            if (currentBallastVolume > 0) {
                //Ballast gets less effective at applying positive lift at higher altitudes.
                //This prevents blimps from ascending into space.
                //Also take into account motionY, as we should provide less force if we are already going in the same direction.
                if (elevatorAngle < 0) {
                    ballastForce = airDensity * currentBallastVolume * -elevatorAngle / 10D;
                } else if (elevatorAngle > 0) {
                    ballastForce = 1.225 * currentBallastVolume * -elevatorAngle / 10D;
                } else {
                    ballastForce = 1.225 * currentBallastVolume * 10D * -motion.y;
                }
                if (motion.y * ballastForce != 0) {
                    ballastForce /= Math.pow(1 + Math.abs(motion.y), 2);
                }
            }

            //Get all other forces.
            wingForce = 0.5F * airDensity * axialVelocity * axialVelocity * currentWingArea * wingLiftCoeff;
            aileronForce = 0.5F * airDensity * axialVelocity * axialVelocity * currentAileronArea * aileronLiftCoeff;
            elevatorForce = 0.5F * airDensity * axialVelocity * axialVelocity * currentElevatorArea * elevatorLiftCoeff;
            rudderForce = 0.5F * airDensity * axialVelocity * axialVelocity * currentRudderArea * rudderLiftCoeff;

            //Get torques.  Point for ailerons is 0.75% to the edge of the wing.
            aileronTorque = aileronForce * currentWingSpan * 0.5F * 0.75F;
            elevatorTorque = elevatorForce * definition.motorized.tailDistance;
            rudderTorque = rudderForce * definition.motorized.tailDistance;

            //If the elevator torque is low, don't apply it.  This prevents elevators from
            //having effects at slow speeds.  We use a faux-torque here from the main plane
            //body to check if we are below this point.
            if (Math.abs(elevatorTorque) < 2D * currentMass / 400D) {
                elevatorTorque = 0;
            }

            //Do more blimp-specific things for the forces.
            if (definition.motorized.isBlimp) {
                //Roll and pitch are applied only if we aren't level.
                //This only happens if we fall out of the sky and land on the ground and tilt.
                if (orientation.angles.z > 0) {
                    aileronTorque = -Math.min(0.5F, orientation.angles.z) * currentMass / 100;
                } else if (orientation.angles.z < 0) {
                    aileronTorque = -Math.max(-0.5F, orientation.angles.z) * currentMass / 100;
                } else {
                    aileronTorque = 0;
                }
                if (orientation.angles.x > 0) {
                    elevatorTorque = -Math.min(0.5F, orientation.angles.x) * currentMass / 100;
                } else if (orientation.angles.x < 0) {
                    elevatorTorque = -Math.max(-0.5F, orientation.angles.x) * currentMass / 100;
                } else {
                    elevatorTorque = 0;
                }

                //If we are turning with the rudder, don't let us heel out of line easily.
                //Rudder force should be minimal for blimps due to their moment of inertia.
                if (rudderTorque * rudderAngle <= 0) {
                    rudderTorque = 0;
                }
            }

            //As a special case, if the vehicle is a stalled plane, add a forwards pitch to allow the plane to right itself.
            //This is needed to prevent the plane from getting stuck in a vertical position and crashing.
            if (currentWingArea > 0 && trackAngle > 40 && orientation.angles.x < 45 && motion.y < -0.1 && groundDeviceCollective.isAnythingOnGround()) {
                elevatorTorque += 100;
            }

            //If we are damaged, don't apply control surface and ballast forces.
            if (damageAmount == definition.general.health) {
                wingForce = 0;
                elevatorForce = 0;
                aileronForce = 0;
                rudderForce = 0;
                elevatorTorque = 0;
                aileronTorque = 0;
                rudderTorque = 0;
                ballastForce = 0;
            }

            //Add all forces to the main force matrix and apply them.
            totalForce.set(0D, wingForce - elevatorForce, 0D).rotate(orientation);
            totalForce.add(thrustForce);
            totalForce.addScaled(normalizedVelocityVector, -dragForce);
            totalForce.y += ballastForce - gravitationalForce;
            motion.addScaled(totalForce, 1 / currentMass);

            //Add all torques to the main torque matrix and apply them.
            totalTorque.set(elevatorTorque, rudderTorque, aileronTorque).add(thrustTorque).scale(180D / Math.PI);
            totalTorque.x /= momentPitch;
            totalTorque.y /= momentYaw;
            totalTorque.z /= momentRoll;
            rotation.angles.set(totalTorque).add(rotorRotation);
        } else if (!lockedOnRoad) {
            towedByConnection.hookupPriorPosition.set(towedByConnection.hookupCurrentPosition);

            //If we are a trailer that is mounted, just orient the vehicle to the exact position of the trailer connection.
            //Otherwise, do is relative to the vehicle orientations.
            if (towedByConnection.hitchConnection.mounted || towedByConnection.hitchConnection.restricted) {
                rotation.set(towedByConnection.towingEntity.orientation).multiply(towedByConnection.hitchConnection.rot);
                if (towedByConnection.hitchConnection.restricted) {
                    rotation.angles.x = orientation.angles.x;
                    rotation.angles.z = orientation.angles.z;
                    rotation.updateToAngles();
                }
                towedByConnection.hookupCurrentPosition.set(towedByConnection.hookupConnection.pos).multiply(towedByConnection.towedEntity.scale).rotate(rotation).add(towedByConnection.towedEntity.position);
            } else {
                //Need to apply both motion to move the trailer, and yaw to adjust the trailer's angle relative to the truck.
                //Yaw is applied based on the current and next position of the truck's hookup.
                //Motion is applied after yaw corrections to ensure the trailer follows the truck.
                //Start by getting the hitch offsets.  We save the current offset as we'll change it for angle calculations.
                //For these offsets, we want them to be local to our coordinates, as that is what system we will need to apply yaw in.
                hitchPrevOffset.set(towedByConnection.hitchPriorPosition).subtract(prevPosition).reOrigin(orientation);
                hitchCurrentOffset.set(towedByConnection.hitchCurrentPosition).subtract(position).reOrigin(orientation);

                //Calculate how much yaw we need to apply to rotate ourselves to match the hitch point.
                hitchPrevOffset.y = 0;
                hitchCurrentOffset.y = 0;
                hitchPrevOffset.normalize();
                hitchCurrentOffset.normalize();
                double rotationDelta = Math.toDegrees(Math.acos(hitchPrevOffset.dotProduct(hitchCurrentOffset, true)));
                if (hitchPrevOffset.crossProduct(hitchCurrentOffset).y < 0) {
                    rotationDelta = -rotationDelta;
                }
                rotation.angles.set(0, rotationDelta, 0);
                rotation.updateToAngles();
                towedByConnection.hookupCurrentPosition.set(towedByConnection.hookupConnection.pos).multiply(towedByConnection.towedEntity.scale).rotate(towedByConnection.towedEntity.orientation).rotate(rotation).add(towedByConnection.towedEntity.position);
            }
            //Now get positional delta.  This assumes perfectly-aligned orientation.				
            motion.set(towedByConnection.hitchCurrentPosition).subtract(towedByConnection.hookupCurrentPosition).scale(1 / speedFactor);
        } else {
            //Towed vehicle on a road with towing vehicle.  Just use same deltas.
            motion.set(towedByConnection.towingVehicle.motion);
            rotation.angles.set(0, 0, 0);
        }
    }

    @Override
    protected void adjustControlSurfaces() {
        if (!definition.motorized.isAircraft && autopilotSetting != 0) {
            //Car, do cruise control.
            if (velocity < autopilotSetting) {
                if (throttle < MAX_THROTTLE) {
                    throttle += MAX_THROTTLE / 100D;
                    setVariable(THROTTLE_VARIABLE, throttle);
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, THROTTLE_VARIABLE, MAX_THROTTLE / 100D));
                }
            } else if (velocity > autopilotSetting) {
                if (throttle > 0) {
                    throttle -= MAX_THROTTLE / 100D;
                    setVariable(THROTTLE_VARIABLE, throttle);
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, THROTTLE_VARIABLE, -MAX_THROTTLE / 100D));
                }
            }
        }

        if (hasRotors) {
            //Helicopter.  Do auto-hover code if required.
            if (autopilotSetting != 0) {
                //Change throttle to maintain altitude.
                //Only do this once every 1/2 second to allow for thrust changes.
                if (ticksExisted % 10 == 0) {
                    if (motion.y < 0 && throttle < MAX_THROTTLE) {
                        throttle += MAX_THROTTLE / 100D;
                        setVariable(THROTTLE_VARIABLE, throttle);
                        InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, THROTTLE_VARIABLE, MAX_THROTTLE / 100D));
                    } else if (motion.y > 0 && throttle < MAX_THROTTLE) {
                        throttle -= MAX_THROTTLE / 100D;
                        setVariable(THROTTLE_VARIABLE, throttle);
                        InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, THROTTLE_VARIABLE, -MAX_THROTTLE / 100D));
                    }
                }
                //Change pitch/roll based on movement.
                double forwardsVelocity = motion.dotProduct(headingVector, false);
                double sidewaysVelocity = motion.dotProduct(sideVector, false);
                double forwardsDelta = forwardsVelocity - prevMotion.dotProduct(headingVector, false);
                double sidewaysDelta = sidewaysVelocity - prevMotion.dotProduct(sideVector, false);
                if (forwardsDelta > 0 && forwardsVelocity > 0 && elevatorTrim < MAX_ELEVATOR_TRIM) {
                    setVariable(ELEVATOR_TRIM_VARIABLE, elevatorTrim + 1);
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_TRIM_VARIABLE, 1));
                } else if (forwardsDelta < 0 && forwardsVelocity < 0 && elevatorTrim > -MAX_ELEVATOR_TRIM) {
                    setVariable(ELEVATOR_TRIM_VARIABLE, elevatorTrim - 1);
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_TRIM_VARIABLE, -1));
                }
                if (sidewaysVelocity > 0 && sidewaysDelta > 0 && aileronTrim < MAX_AILERON_TRIM) {
                    setVariable(AILERON_TRIM_VARIABLE, aileronTrim + 1);
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_TRIM_VARIABLE, 1));
                } else if (sidewaysVelocity < 0 && sidewaysDelta < 0 && aileronTrim > -MAX_AILERON_TRIM) {
                    setVariable(AILERON_TRIM_VARIABLE, aileronTrim - 1);
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_TRIM_VARIABLE, -1));
                }
            } else {
                //Reset trim to prevent directional surges.
                if (elevatorTrim < 0) {
                    setVariable(ELEVATOR_TRIM_VARIABLE, elevatorTrim + 1);
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_TRIM_VARIABLE, 1));
                } else if (elevatorTrim > 0) {
                    setVariable(ELEVATOR_TRIM_VARIABLE, elevatorTrim - 1);
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_TRIM_VARIABLE, -1));
                }
                if (aileronTrim < 0) {
                    setVariable(AILERON_TRIM_VARIABLE, aileronTrim + 1);
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_TRIM_VARIABLE, 1));
                } else if (aileronTrim > 0) {
                    setVariable(AILERON_TRIM_VARIABLE, aileronTrim - 1);
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_TRIM_VARIABLE, -1));
                }
            }
        } else if (definition.motorized.isAircraft && autopilotSetting != 0) {
            //Normal aircraft.  Do autopilot operations if required.
            //If we are not flying at a steady elevation, angle the elevator to compensate
            if (-motion.y * 100 > elevatorTrim + 1 && elevatorTrim < MAX_ELEVATOR_TRIM) {
                setVariable(ELEVATOR_TRIM_VARIABLE, elevatorTrim + 0.1);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_TRIM_VARIABLE, 0.1));
            } else if (-motion.y * 100 < elevatorTrim - 1 && elevatorTrim > -MAX_ELEVATOR_TRIM) {
                setVariable(ELEVATOR_TRIM_VARIABLE, elevatorTrim - 0.1);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_TRIM_VARIABLE, -0.1));
            }
            //Keep the roll angle at 0.
            if (-orientation.angles.z > aileronTrim + 0.1 && aileronTrim < MAX_AILERON_TRIM) {
                setVariable(AILERON_TRIM_VARIABLE, aileronTrim + 0.1);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_TRIM_VARIABLE, 0.1));
            } else if (-orientation.angles.z < aileronTrim - 0.1 && aileronTrim > -MAX_AILERON_TRIM) {
                setVariable(AILERON_TRIM_VARIABLE, aileronTrim - 0.1);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_TRIM_VARIABLE, -0.1));
            }
        }

        //If we don't have controllers, reset control states to 0.
        //Don't do this on roads, since those manually set our controls.
        if (!lockedOnRoad && controllerCount == 0) {
            if (aileronInput > AILERON_DAMPEN_RATE) {
                setVariable(AILERON_INPUT_VARIABLE, aileronInput - AILERON_DAMPEN_RATE);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_INPUT_VARIABLE, -AILERON_DAMPEN_RATE, 0, MAX_AILERON_ANGLE));
            } else if (aileronInput < -AILERON_DAMPEN_RATE) {
                setVariable(AILERON_INPUT_VARIABLE, aileronInput + AILERON_DAMPEN_RATE);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_INPUT_VARIABLE, AILERON_DAMPEN_RATE, -MAX_AILERON_ANGLE, 0));
            } else if (aileronInput != 0) {
                setVariable(AILERON_INPUT_VARIABLE, 0);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableSet(this, AILERON_INPUT_VARIABLE, 0));
            }

            if (elevatorInput > ELEVATOR_DAMPEN_RATE) {
                setVariable(ELEVATOR_INPUT_VARIABLE, elevatorInput - ELEVATOR_DAMPEN_RATE);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_INPUT_VARIABLE, -ELEVATOR_DAMPEN_RATE, 0, MAX_ELEVATOR_ANGLE));
            } else if (elevatorInput < -ELEVATOR_DAMPEN_RATE) {
                setVariable(ELEVATOR_INPUT_VARIABLE, elevatorInput + ELEVATOR_DAMPEN_RATE);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_INPUT_VARIABLE, ELEVATOR_DAMPEN_RATE, -MAX_ELEVATOR_ANGLE, 0));
            } else if (elevatorInput != 0) {
                setVariable(ELEVATOR_INPUT_VARIABLE, 0);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableSet(this, ELEVATOR_INPUT_VARIABLE, 0));
            }

            if (rudderInput > RUDDER_DAMPEN_RATE) {
                setVariable(RUDDER_INPUT_VARIABLE, rudderInput - RUDDER_DAMPEN_RATE);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, RUDDER_INPUT_VARIABLE, -RUDDER_DAMPEN_RATE, 0, MAX_RUDDER_ANGLE));
            } else if (rudderInput < -RUDDER_DAMPEN_RATE) {
                setVariable(RUDDER_INPUT_VARIABLE, rudderInput + RUDDER_DAMPEN_RATE);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, RUDDER_INPUT_VARIABLE, RUDDER_DAMPEN_RATE, -MAX_RUDDER_ANGLE, 0));
            } else if (rudderInput != 0) {
                setVariable(RUDDER_INPUT_VARIABLE, 0);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableSet(this, RUDDER_INPUT_VARIABLE, 0));
            }

        }
    }

    protected double getRecursiveTowingThrust() {
        if (!towingConnections.isEmpty()) {
            double thrust = 0;
            for (TowingConnection connection : towingConnections) {
                if (!connection.hitchConnection.mounted) {
                    thrust += connection.towedVehicle.thrustForceValue + connection.towedVehicle.getRecursiveTowingThrust();
                }
            }
            return thrust;
        } else {
            return 0;
        }
    }

    protected static double getLiftCoeff(double angleOfAttack, double maxLiftCoeff) {
        if (angleOfAttack == 0) {
            return 0;
        } else if (Math.abs(angleOfAttack) <= 15 * 1.25) {
            return maxLiftCoeff * Math.sin(Math.PI / 2 * angleOfAttack / 15);
        } else if (Math.abs(angleOfAttack) <= 15 * 1.5) {
            if (angleOfAttack > 0) {
                return maxLiftCoeff * (0.4 + 1 / (angleOfAttack - 15));
            } else {
                return maxLiftCoeff * (-0.4 + 1 / (angleOfAttack + 15));
            }
        } else {
            return maxLiftCoeff * Math.sin(Math.PI / 6 * angleOfAttack / 15);
        }
    }

    @Override
    public boolean shouldRenderBeams() {
        return ConfigSystem.client.renderingSettings.vehicleBeams.value;
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        //If we are a forwarded variable and are a connected trailer, do that now.
        if (definition.motorized.isTrailer && towedByConnection != null && definition.motorized.hookupVariables.contains(variable)) {
            return towedByConnection.towingVehicle.getRawVariableValue(variable, partialTicks);
        }

        //Not a part of a forwarded variable.  Just return normally.
        switch (variable) {
            //Vehicle world state cases.
            case ("yaw"):
                return orientation.angles.y;
            case ("heading"):
                double heading = -orientation.angles.y;
                if (ConfigSystem.client.controlSettings.north360.value)
                    heading += 180;
                while (heading < 0)
                    heading += 360;
                while (heading > 360)
                    heading -= 360;
                return heading;
            case ("pitch"):
                return orientation.angles.x;
            case ("roll"):
                return orientation.angles.z;
            case ("altitude"):
                return position.y;
            case ("speed"):
                return axialVelocity * speedFactor * 20;
            case ("speed_scaled"):
                return axialVelocity * 20;
            case ("speed_factor"):
                return speedFactor;
            case ("acceleration"):
                return motion.length() - prevMotion.length();
            case ("road_angle_front"):
                return frontFollower != null ? frontFollower.getCurrentYaw() - orientation.angles.y : 0;
            case ("road_angle_rear"):
                return rearFollower != null ? rearFollower.getCurrentYaw() - orientation.angles.y : 0;

            //Vehicle state cases.
            case ("fuel"):
                return fuelTank.getFluidLevel() / fuelTank.getMaxLevel();
            case ("mass"):
                return currentMass;
            case ("electric_power"):
                return electricPower;
            case ("electric_usage"):
                return electricFlow * 20D;
            case ("engines_on"):
                return enginesOn ? 1 : 0;
            case ("engines_running"):
                return enginesRunning ? 1 : 0;
            case ("reverser"):
                return reverseThrust ? 1 : 0;
            case ("locked"):
                return locked ? 1 : 0;
            case ("door"):
                return parkingBrakeOn && velocity < 0.25 ? 1 : 0;
            case ("fueling"):
                return beingFueled ? 1 : 0;

            //State cases generally used on aircraft.
            case ("flaps_actual"):
                return flapCurrentAngle;
            case ("flaps_moving"):
                return flapCurrentAngle != flapDesiredAngle ? 1 : 0;
            case ("vertical_speed"):
                return motion.y * speedFactor * 20;
            case ("lift_reserve"):
                return -trackAngle;
            case ("turn_coordinator"):
                return ((rotation.angles.z) / 10 + rotation.angles.y) / 0.15D * 25;
            case ("turn_indicator"):
                return (rotation.angles.y) / 0.15F * 25F;
            case ("slip"):
                return 75 * sideVector.dotProduct(normalizedVelocityVector, true);
            case ("gear_moving"):
                return (isVariableActive(GEAR_VARIABLE) ? gearMovementTime != definition.motorized.gearSequenceDuration : gearMovementTime != 0) ? 1 : 0;
            case ("beacon_direction"):
                return selectedBeacon != null ? orientation.angles.getClampedYDelta(Math.toDegrees(Math.atan2(selectedBeacon.position.x - position.x, selectedBeacon.position.z - position.z))) : 0;
            case ("beacon_bearing_setpoint"):
                return selectedBeacon != null ? selectedBeacon.bearing : 0;
            case ("beacon_bearing_delta"):
                return selectedBeacon != null ? selectedBeacon.getBearingDelta(this) : 0;
            case ("beacon_glideslope_setpoint"):
                return selectedBeacon != null ? selectedBeacon.glideSlope : 0;
            case ("beacon_glideslope_actual"):
                return selectedBeacon != null ? Math.toDegrees(Math.asin((position.y - selectedBeacon.position.y) / position.distanceTo(selectedBeacon.position))) : 0;
            case ("beacon_glideslope_delta"):
                return selectedBeacon != null ? selectedBeacon.glideSlope - Math.toDegrees(Math.asin((position.y - selectedBeacon.position.y) / position.distanceTo(selectedBeacon.position))) : 0;

            default: {
                //Missile incoming variables.
                //Variable is in the form of missile_X_variablename.
                if (variable.startsWith("missile_")) {
                    String missileVariable = variable.substring(variable.lastIndexOf("_") + 1);
                    int missileNumber = getVariableNumber(variable.substring(0, variable.lastIndexOf('_')));
                    if (missileNumber != -1) {
                        if (missilesIncoming.size() <= missileNumber) {
                            return 0;
                        } else {
                            switch (missileVariable) {
                                case ("distance"):
                                    return missilesIncoming.get(missileNumber).targetDistance;
                                case ("direction"): {
                                    Point3D missilePos = missilesIncoming.get(missileNumber).position;
                                    return Math.toDegrees(Math.atan2(-missilePos.z + position.z, -missilePos.x + position.x)) + 90 + orientation.angles.y;
                                }
                            }
                        }
                    } else if (missileVariable.equals("incoming")) {
                        return missilesIncoming.isEmpty() ? 0 : 1;
                    }
                }
            }
        }

        //Not a vehicle variable or a part variable.  We could have an error, but likely we have an older pack,
        //a closed door, a missing part, a custom variable that's not on, or something else entirely.
        //Just return super here.
        return super.getRawVariableValue(variable, partialTicks);
    }

    @Override
    public void renderBoundingBoxes(TransformationMatrix transform) {
        super.renderBoundingBoxes(transform);
        if (towedByConnection == null || !towedByConnection.hitchConnection.mounted) {
            for (BoundingBox box : groundDeviceCollective.getGroundBounds()) {
                box.renderWireframe(this, transform, null, ColorRGB.BLUE);
            }
        }
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setDouble("flapCurrentAngle", flapCurrentAngle);
        return data;
    }
}
