package minecrafttransportsimulator.entities.instances;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TowingConnection;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
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
    public final ComputedVariable aileronInputVar;
    public final ComputedVariable aileronAngleVar;
    public final ComputedVariable aileronTrimVar;
    public final ComputedVariable aileronAreaVar;
    public static final double MAX_AILERON_ANGLE = 25;
    public static final double MAX_AILERON_TRIM = 10;
    public static final double AILERON_DAMPEN_RATE = 0.6;

    //Elevator.
    public final ComputedVariable elevatorInputVar;
    public final ComputedVariable elevatorAngleVar;
    public final ComputedVariable elevatorTrimVar;
    public final ComputedVariable elevatorAreaVar;
    public static final double MAX_ELEVATOR_ANGLE = 25;
    public static final double MAX_ELEVATOR_TRIM = 10;
    public static final double ELEVATOR_DAMPEN_RATE = 0.6;

    //Rudder.
    public final ComputedVariable rudderInputVar;
    public final ComputedVariable rudderAngleVar;
    public final ComputedVariable rudderTrimVar;
    public final ComputedVariable rudderAreaVar;
    public static final double MAX_RUDDER_ANGLE = 45;
    public static final double MAX_RUDDER_TRIM = 10;
    public static final double RUDDER_DAMPEN_RATE = 2.0;
    public static final double RUDDER_DAMPEN_RETURN_RATE = 4.0;

    //Wings/flaps.
    public static final short MAX_FLAP_ANGLE_REFERENCE = 350;
    public final ComputedVariable wingAreaVar;
    public final ComputedVariable wingSpanVar;
    public final ComputedVariable flapDesiredAngleVar;
    public final ComputedVariable flapActualAngleVar;
    
    //Autopilot.
    public final ComputedVariable autopilotValueVar;
    public final ComputedVariable autolevelEnabledVar;

    //External state control.
    public boolean turningLeft;
    public boolean turningRight;
    public byte turningCooldown;
    public int repairCooldownTicks;
    public double airDensity;
    public double seaLevel = ConfigSystem.settings.general.seaLevel.value;
    public int controllerCount;
    public IWrapperPlayer lastController;

    //Internal states.
    public double indicatedSpeed;
    private boolean hasRotors;
    private double trackAngle;
    private final Point3D normalizedVelocityVector = new Point3D();
    private final Point3D verticalVector = new Point3D();
    private final Point3D sideVector = new Point3D();
    private final Point3D hitchPrevOffset = new Point3D();
    private final Point3D hitchCurrentOffset = new Point3D();
    private final Set<AEntityG_Towable<?>> towedEntitiesCheckedForWeights = new HashSet<>();

    //Physics properties
    public final ComputedVariable dragCoefficientVar;
    public final ComputedVariable ballastVolumeVar;
    public final ComputedVariable waterBallastFactorVar;
    public final ComputedVariable axleRatioVar;

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

    public EntityVehicleF_Physics(AWrapperWorld world, IWrapperPlayer placingPlayer, ItemVehicle item, IWrapperNBT data) {
        super(world, placingPlayer, item, data);
        addVariable(this.aileronInputVar = new ComputedVariable(this, "input_aileron", data));
        addVariable(this.aileronAngleVar = new ComputedVariable(this, "aileron", data));
        addVariable(this.aileronTrimVar = new ComputedVariable(this, "trim_aileron", data));
        addVariable(this.aileronAreaVar = new ComputedVariable(this, "aileronArea"));

        addVariable(this.elevatorInputVar = new ComputedVariable(this, "input_elevator", data));
        addVariable(this.elevatorAngleVar = new ComputedVariable(this, "elevator", data));
        addVariable(this.elevatorTrimVar = new ComputedVariable(this, "trim_elevator", data));
        addVariable(this.elevatorAreaVar = new ComputedVariable(this, "elevatorArea"));

        addVariable(this.rudderInputVar = new ComputedVariable(this, "input_rudder", data));
        addVariable(this.rudderAngleVar = new ComputedVariable(this, "rudder", data));
        addVariable(this.rudderTrimVar = new ComputedVariable(this, "trim_rudder", data));
        addVariable(this.rudderAreaVar = new ComputedVariable(this, "rudderArea"));

        addVariable(this.wingAreaVar = new ComputedVariable(this, "wingArea"));
        addVariable(this.wingSpanVar = new ComputedVariable(this, "wingSpan"));
        addVariable(this.flapDesiredAngleVar = new ComputedVariable(this, "flaps_setpoint", data));
        addVariable(this.flapActualAngleVar = new ComputedVariable(this, "flaps_actual", data));

        addVariable(this.autopilotValueVar = new ComputedVariable(this, "autopilot", data));
        addVariable(this.autolevelEnabledVar = new ComputedVariable(this, "auto_level", data));

        addVariable(this.dragCoefficientVar = new ComputedVariable(this, "dragCoefficient"));
        addVariable(this.ballastVolumeVar = new ComputedVariable(this, "ballastVolume"));
        addVariable(this.waterBallastFactorVar = new ComputedVariable(this, "waterBallastFactor"));
        addVariable(this.axleRatioVar = new ComputedVariable(this, "axleRatio"));
    }

    @Override
    public void update() {
        super.update();
        world.beginProfiling("VehicleF_Level", true);
        if (repairCooldownTicks > 0) {
            --repairCooldownTicks;
        }
        indicatedSpeed = axialVelocity * speedFactor * 20;

        //Set vectors.
        verticalVector.set(0D, 1D, 0D).rotate(orientation);
        normalizedVelocityVector.set(motion).normalize();
        sideVector.set(verticalVector.crossProduct(headingVector));
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
        return -rudderInputVar.currentValue / (float) MAX_RUDDER_ANGLE;
    }

    @Override
    protected void addToSteeringAngle(double degrees) {
        //Invert the degrees, as rudder is inverted from normal steering.
        double delta;
        if (rudderInputVar.currentValue - degrees > MAX_RUDDER_ANGLE) {
            delta = MAX_RUDDER_ANGLE - rudderInputVar.currentValue;
        } else if (rudderInputVar.currentValue - degrees < -MAX_RUDDER_ANGLE) {
            delta = -MAX_RUDDER_ANGLE - rudderInputVar.currentValue;
        } else {
            delta = -degrees;
        }
        rudderInputVar.adjustBy(delta, true);
    }

    @Override
    public void setVariableDefaults() {
        super.setVariableDefaults();
        aileronAreaVar.setTo(definition.motorized.aileronArea, false);
        aileronAngleVar.setTo(aileronInputVar.currentValue, false);
        elevatorAngleVar.setTo(elevatorInputVar.currentValue, false);
        elevatorAreaVar.setTo(definition.motorized.elevatorArea, false);
        rudderAngleVar.setTo(rudderInputVar.currentValue, false);
        rudderAreaVar.setTo(definition.motorized.rudderArea, false);

        wingAreaVar.setTo(definition.motorized.wingArea + definition.motorized.wingArea * 0.15F * flapActualAngleVar.currentValue / MAX_FLAP_ANGLE_REFERENCE, false);
        wingSpanVar.setTo(definition.motorized.wingSpan, false);
        //Run flap defaults after wings.  This lets wings get the last-state value, which might have been modified post-default-value setting.
        if (definition.motorized.flapNotches != null && !definition.motorized.flapNotches.isEmpty()) {
            if (flapActualAngleVar.currentValue < flapDesiredAngleVar.currentValue) {
                flapActualAngleVar.setTo(flapActualAngleVar.currentValue + definition.motorized.flapSpeed, false);
            } else if (flapActualAngleVar.currentValue > flapDesiredAngleVar.currentValue) {
                flapActualAngleVar.setTo(flapActualAngleVar.currentValue - definition.motorized.flapSpeed, false);
            }
            if (Math.abs(flapActualAngleVar.currentValue - flapDesiredAngleVar.currentValue) < definition.motorized.flapSpeed) {
                flapActualAngleVar.setTo(flapDesiredAngleVar.currentValue, false);
            }
        }

        dragCoefficientVar.setTo(definition.motorized.dragCoefficient, false);
        ballastVolumeVar.setTo(definition.motorized.ballastVolume, false);
        waterBallastFactorVar.setTo(definition.motorized.waterBallastFactor, false);
        axleRatioVar.setTo(definition.motorized.axleRatio, false);
    }

    @Override
    protected void getForcesAndMotions() {
        //Get engine thrust force contributions.  This happens for all vehicles, towed or not.
        //The only exception are mounted towed vehicles, which are static.
        hasRotors = false;
        thrustForce.set(0, 0, 0);
        thrustTorque.set(0, 0, 0);
        rotorRotation.set(0, 0, 0);
        thrustForceValue = 0;
        if (towedByConnection == null || !towedByConnection.hitchConnection.mounted) {
            for (APart part : allParts) {
                if (part instanceof PartEngine) {
                    thrustForceValue += ((PartEngine) part).addToForceOutput(thrustForce, thrustTorque);
                } else if (part instanceof PartPropeller) {
                    PartPropeller propeller = (PartPropeller) part;
                    thrustForceValue += propeller.addToForceOutput(thrustForce, thrustTorque);
                    if (propeller.definition.propeller.isRotor && groundDeviceCollective.isAnythingOnGround()) {
                        hasRotors = true;
                        if (!autopilotValueVar.isActive && autolevelEnabledVar.isActive) {
                            rotorRotation.set((-(elevatorAngleVar.currentValue + elevatorTrimVar.currentValue) - orientation.angles.x) / MAX_ELEVATOR_ANGLE, -5D * rudderAngleVar.currentValue / MAX_RUDDER_ANGLE, ((aileronAngleVar.currentValue + aileronTrimVar.currentValue) - orientation.angles.z) / MAX_AILERON_ANGLE);
                        } else {
                            if (!autopilotValueVar.isActive) {
                                rotorRotation.set(-5D * elevatorAngleVar.currentValue / MAX_ELEVATOR_ANGLE, -5D * rudderAngleVar.currentValue / MAX_RUDDER_ANGLE, 5D * aileronAngleVar.currentValue / MAX_AILERON_ANGLE);
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
                                rotorRotation.y = -5D * rudderAngleVar.currentValue / MAX_RUDDER_ANGLE;
                            }
                        }
                    }
                }
            }
        }

        //If we are free, do normal updates.  But if we are towed by a vehicle, do trailer forces instead.
        //This prevents trailers from behaving badly and flinging themselves into the abyss.
        if (towedByConnection == null) {
            //Set moments and air density.
            airDensity = 1.225 * Math.pow(2, -(position.y-seaLevel) / (500D * world.getMaxHeight() / 256D));
            momentRoll = definition.motorized.emptyMass * (1.5F + fuelTank.getFluidLevel() / 10000F);
            momentPitch = 2D * currentMass;
            momentYaw = 3D * currentMass;

            //If we are towing any non-mounted vehicles, get their thrust contributions as well.
            double towedThrust = getRecursiveTowingThrust();
            if (towedThrust != 0) {
                towingThrustForce.set(0, 0, towedThrust).rotate(orientation);
                thrustForce.add(towingThrustForce);
            }

            //Get the track angle.  This is used for control surfaces.
            trackAngle = -Math.toDegrees(Math.asin(verticalVector.dotProduct(normalizedVelocityVector, true)));

            //Set blimp-specific states before calculating forces.
            if (definition.motorized.isBlimp) {
                thrustTorque.x = 0;
                thrustTorque.z = 0;
                //If we have the brake pressed at a slow speed, stop the blimp.
                //This is needed to prevent runaway blimps.
                if (Math.hypot(motion.x, motion.z) < 0.15 && (brakeVar.isActive || parkingBrakeVar.isActive)) {
                    motion.x = 0;
                    motion.z = 0;
                    thrustForce.set(0D, 0D, 0D);
                    thrustTorque.set(0D, 0D, 0D);
                }
            }

            //Get the lift coefficients and states for control surfaces.
            double yawAngleDelta = Math.toDegrees(Math.asin(sideVector.dotProduct(normalizedVelocityVector, true)));
            wingLiftCoeff = getLiftCoeff(trackAngle, 2 + flapActualAngleVar.currentValue / MAX_FLAP_ANGLE_REFERENCE);
            aileronLiftCoeff = getLiftCoeff((aileronAngleVar.currentValue + aileronTrimVar.currentValue), 2);
            elevatorLiftCoeff = getLiftCoeff(-2.5 + trackAngle - (elevatorAngleVar.currentValue + elevatorTrimVar.currentValue), 2);
            rudderLiftCoeff = getLiftCoeff(yawAngleDelta - (rudderAngleVar.currentValue + rudderTrimVar.currentValue), 2);

            //Get the drag coefficient and force.
            if (definition.motorized.isBlimp) {
                dragCoeff = 0.004F * Math.pow(Math.abs(yawAngleDelta), 2) + dragCoefficientVar.currentValue;
            } else if (definition.motorized.isAircraft) {
                //Aircraft are 0.03 by default, or whatever is specified.
                dragCoeff = 0.0004F * Math.pow(trackAngle, 2) + dragCoefficientVar.currentValue;
            } else {
                dragCoeff = dragCoefficientVar.currentValue;
                //If we aren't an aircraft, check for grounded ground devices.
                //If we don't have any grounded ground devices, assume we are in the air or in water.
                //This results in an increase in drag due to poor airflow.
                if (groundDeviceCollective.groundedGroundDevices.isEmpty()) {
                    dragCoeff *= 3D;
                }
            }
            if (definition.motorized.crossSectionalArea > 0) {
                dragForce = 0.5F * airDensity * velocity * velocity * definition.motorized.crossSectionalArea * dragCoeff;
            } else if (wingSpanVar.currentValue > 0) {
                dragForce = 0.5F * airDensity * velocity * velocity * wingAreaVar.currentValue * (dragCoeff + wingLiftCoeff * wingLiftCoeff / (Math.PI * wingSpanVar.currentValue * wingSpanVar.currentValue / wingAreaVar.currentValue * 0.8));
            } else {
                dragForce = 0.5F * airDensity * velocity * velocity * 5.0F * dragCoeff;
            }

            //Get ballast force.
            if (ballastVolumeVar.currentValue > 0) {
                //Ballast gets less effective at applying positive lift at higher altitudes.
                //This prevents blimps from ascending into space.
                //Also take into account motionY, as we should provide less force if we are already going in the same direction.
                if (elevatorAngleVar.currentValue < 0) {
                    ballastForce = airDensity * ballastVolumeVar.currentValue * -elevatorAngleVar.currentValue / 10D;
                } else if (elevatorAngleVar.currentValue > 0) {
                    ballastForce = 1.225 * ballastVolumeVar.currentValue * -elevatorAngleVar.currentValue / 10D;
                } else if (motion.y < -0.15 || motion.y > 0.15) {
                    ballastForce = 1.225 * ballastVolumeVar.currentValue * 10D * -motion.y;
                } else {
                    ballastForce = 0;
                    motion.y = 0;
                }
                if (motion.y * ballastForce != 0) {
                    ballastForce /= Math.pow(1 + Math.abs(motion.y), 2);
                }
            }

            //Get all other forces.
            wingForce = 0.5F * airDensity * axialVelocity * axialVelocity * wingAreaVar.currentValue * wingLiftCoeff;
            aileronForce = 0.5F * airDensity * axialVelocity * axialVelocity * aileronAreaVar.currentValue * aileronLiftCoeff;
            elevatorForce = 0.5F * airDensity * axialVelocity * axialVelocity * elevatorAreaVar.currentValue * elevatorLiftCoeff;
            rudderForce = 0.5F * airDensity * axialVelocity * axialVelocity * rudderAreaVar.currentValue * rudderLiftCoeff;

            //Get torques.  Point for ailerons is 0.75% to the edge of the wing.
            aileronTorque = aileronForce * wingSpanVar.currentValue * 0.5F * 0.75F;
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
                if (rudderTorque * rudderAngleVar.currentValue > 0) {
                    rudderTorque = 0;
                }
            }

            //As a special case, if the vehicle is a stalled plane, add a forwards pitch to allow the plane to right itself.
            //This is needed to prevent the plane from getting stuck in a vertical position and crashing.
            if (wingAreaVar.currentValue > 0 && trackAngle > 40 && orientation.angles.x < 45 && motion.y < -0.1 && groundDeviceCollective.isAnythingOnGround()) {
                elevatorTorque += 100;
            }

            //If we are dead, don't apply forces.
            if (outOfHealth) {
                wingForce = 0;
                elevatorForce = 0;
                aileronForce = 0;
                rudderForce = 0;
                elevatorTorque = 0;
                aileronTorque = 0;
                rudderTorque = 0;
                ballastForce = 0;
            }

            //Finally, get gravity.  Blimps sink when dead.
            gravitationalForce = !ballastVolumeVar.isActive || outOfHealth ? currentMass * (9.8 / 400) : 0;
            if (waterBallastFactorVar.isActive && world.isBlockLiquid(position)) {
                gravitationalForce -= gravitationalForce * waterBallastFactorVar.currentValue;
                elevatorTorque = -orientation.angles.x * 2;
                aileronTorque = -orientation.angles.z * 2;
            }
            if (!definition.motorized.isAircraft) {
                gravitationalForce *= ConfigSystem.settings.general.gravityFactor.value;
            }

            //Add all forces to the main force matrix and apply them.
            if (ConfigSystem.settings.general.maxFlightHeight.value > 0 && position.y > ConfigSystem.settings.general.maxFlightHeight.value) {
                wingForce = 0;
                thrustForce.y = 0;
            }
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
                rotation.set(towedByConnection.towingEntity.orientation);
                if (towedByConnection.hitchConnection.rot != null) {
                    rotation.multiply(towedByConnection.hitchConnection.rot);
                }
                if (towedByConnection.hookupConnection.rot != null) {
                    rotation.multiply(towedByConnection.hookupConnection.rot);
                }
                if (towedByConnection.hitchConnection.restricted) {
                    rotation.angles.x = orientation.angles.x;
                    rotation.angles.z = orientation.angles.z;
                    rotation.updateToAngles();
                }
                towedByConnection.hookupCurrentPosition.set(towedByConnection.hookupConnection.pos).multiply(towedByConnection.towedEntity.scale).rotate(rotation).add(towedByConnection.towedEntity.position);
            } else if (!towedByConnection.hitchPriorPosition.isZero()) {//Can't update on the first tick.
                //Need to apply both motion to move the trailer, and yaw to adjust the trailer's angle relative to the truck.
                //Yaw is applied based on the current and next position of the truck's hookup.
                //Motion is applied after yaw corrections to ensure the trailer follows the truck.
                //Start by getting the hitch offsets.  We save the current offset as we'll change it for angle calculations.
                //For these offsets, we want them to be local to our coordinates, as that is what system we will need to apply yaw in.
                if (towedByConnection.hookupConnection.pos.x != 0) {
                    //Need to offset reference point to account for offset hookup location relative to center-line.
                    hitchPrevOffset.set(-towedByConnection.hookupConnection.pos.x, 0, 0).rotate(prevOrientation);
                    hitchCurrentOffset.set(-towedByConnection.hookupConnection.pos.x, 0, 0).rotate(orientation);

                    hitchPrevOffset.add(towedByConnection.hitchPriorPosition).subtract(prevPosition);
                    hitchCurrentOffset.add(towedByConnection.hitchCurrentPosition).subtract(position);
                } else {
                    hitchPrevOffset.set(towedByConnection.hitchPriorPosition).subtract(prevPosition);
                    hitchCurrentOffset.set(towedByConnection.hitchCurrentPosition).subtract(position);
                }

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

                //Update hookup position now that rotation is current.
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
        if (!definition.motorized.isAircraft && autopilotValueVar.isActive) {
            //Car, do cruise control.
            if (indicatedSpeed < autopilotValueVar.currentValue) {
                if (throttleVar.currentValue < MAX_THROTTLE) {
                	throttleVar.adjustBy(MAX_THROTTLE / 100D, true);
                }
            } else if (indicatedSpeed > autopilotValueVar.currentValue) {
                if (throttleVar.currentValue > 0) {
                	throttleVar.adjustBy(-MAX_THROTTLE / 100D, true);
                }
            }
        }

        if (hasRotors) {
            //Helicopter.  Do auto-hover code if required.
            if (autopilotValueVar.isActive) {
                //Change throttle to maintain altitude.
                //Only do this once every 1/2 second to allow for thrust changes.
                if (ticksExisted % 10 == 0) {
                    if (motion.y < 0 && throttleVar.currentValue < MAX_THROTTLE) {
                    	throttleVar.adjustBy(MAX_THROTTLE / 100D, true);
                    } else if (motion.y > 0 && throttleVar.currentValue < MAX_THROTTLE) {
                    	throttleVar.adjustBy(-MAX_THROTTLE / 100D, true);
                    }
                }
                //Change pitch/roll based on movement.
                double forwardsVelocity = motion.dotProduct(headingVector, false);
                double sidewaysVelocity = motion.dotProduct(sideVector, false);
                double forwardsDelta = forwardsVelocity - prevMotion.dotProduct(headingVector, false);
                double sidewaysDelta = sidewaysVelocity - prevMotion.dotProduct(sideVector, false);
                if (forwardsDelta > 0 && forwardsVelocity > 0 && elevatorTrimVar.currentValue < MAX_ELEVATOR_TRIM) {
                	elevatorTrimVar.adjustBy(1, true);
                } else if (forwardsDelta < 0 && forwardsVelocity < 0 && elevatorTrimVar.currentValue > -MAX_ELEVATOR_TRIM) {
                	elevatorTrimVar.adjustBy(-1, true);
                }
                if (sidewaysVelocity > 0 && sidewaysDelta > 0 && aileronTrimVar.currentValue < MAX_AILERON_TRIM) {
                	aileronTrimVar.adjustBy(1, true);
                } else if (sidewaysVelocity < 0 && sidewaysDelta < 0 && aileronTrimVar.currentValue > -MAX_AILERON_TRIM) {
                	aileronTrimVar.adjustBy(-1, true);
                }
            } else {
                //Reset trim to prevent directional surges.
                if (elevatorTrimVar.currentValue < 0) {
                	elevatorTrimVar.adjustBy(1, true);
                } else if (elevatorTrimVar.currentValue > 0) {
                	elevatorTrimVar.adjustBy(-1, true);
                }
                if (aileronTrimVar.currentValue < 0) {
                	aileronTrimVar.adjustBy(1, true);
                } else if (aileronTrimVar.currentValue > 0) {
                	aileronTrimVar.adjustBy(-1, true);
                }
            }
        } else if (definition.motorized.isAircraft && autopilotValueVar.isActive) {
            //Normal aircraft.  Do autopilot operations if required.
            //If we are not flying at a steady elevation, angle the elevator to compensate
            if (-motion.y * 10 > elevatorTrimVar.currentValue + 1 && elevatorTrimVar.currentValue < MAX_ELEVATOR_TRIM) {
            	elevatorTrimVar.adjustBy(0.1, true);
            } else if (-motion.y * 10 < elevatorTrimVar.currentValue - 1 && elevatorTrimVar.currentValue > -MAX_ELEVATOR_TRIM) {
            	elevatorTrimVar.adjustBy(-0.1, true);
            }
            //Keep the roll angle at 0.
            if (-orientation.angles.z > aileronTrimVar.currentValue + 0.1 && aileronTrimVar.currentValue < MAX_AILERON_TRIM) {
            	aileronTrimVar.adjustBy(0.1, true);
            } else if (-orientation.angles.z < aileronTrimVar.currentValue - 0.1 && aileronTrimVar.currentValue > -MAX_AILERON_TRIM) {
            	aileronTrimVar.adjustBy(-0.1, true);
            }
        }

        //If we don't have controllers, reset control states to 0.
        //Don't do this on roads, since those manually set our controls.
        if (!lockedOnRoad && controllerCount == 0) {
            if (aileronInputVar.currentValue > AILERON_DAMPEN_RATE) {
            	aileronInputVar.increment(-AILERON_DAMPEN_RATE, 0, MAX_AILERON_ANGLE, true);
            } else if (aileronInputVar.currentValue < -AILERON_DAMPEN_RATE) {
                aileronInputVar.increment(AILERON_DAMPEN_RATE, -MAX_AILERON_ANGLE, 0, true);
            } else if (aileronInputVar.currentValue != 0) {
                aileronInputVar.setTo(0, true);
            }

            if (elevatorInputVar.currentValue > ELEVATOR_DAMPEN_RATE) {
                elevatorInputVar.increment(-ELEVATOR_DAMPEN_RATE, 0, MAX_ELEVATOR_ANGLE, true);
            } else if (elevatorInputVar.currentValue < -ELEVATOR_DAMPEN_RATE) {
            	elevatorInputVar.increment(ELEVATOR_DAMPEN_RATE, -MAX_ELEVATOR_ANGLE, 0, true);
            } else if (elevatorInputVar.currentValue != 0) {
                elevatorInputVar.setTo(0, true);
            }

            if (rudderInputVar.currentValue > RUDDER_DAMPEN_RATE) {
                rudderInputVar.increment(-RUDDER_DAMPEN_RATE, 0, MAX_RUDDER_ANGLE, true);
            } else if (rudderInputVar.currentValue < -RUDDER_DAMPEN_RATE) {
            	rudderInputVar.increment(RUDDER_DAMPEN_RATE, -MAX_RUDDER_ANGLE, 0, true);
            } else if (rudderInputVar.currentValue != 0) {
            	rudderInputVar.setTo(0, true);
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
    public ComputedVariable getOrCreateVariable(String variable) {
        //If we are a forwarded variable and are a connected trailer, do that now.
        if (definition.motorized.isTrailer && towedByConnection != null && definition.motorized.hookupVariables.contains(variable)) {
            return towedByConnection.towingVehicle.getOrCreateVariable(variable);
        } else {
            return super.getOrCreateVariable(variable);
        }
    }

    @Override
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {

        //Not a part of a forwarded variable.  Just return new ComputedVariable(this, variable, partialTicks -> normally.
        switch (variable) {
            //Vehicle world state cases.
            case ("yaw"):
                return new ComputedVariable(this, variable, partialTicks -> orientation.angles.y, false);
            case ("heading"):
                return new ComputedVariable(this, variable, partialTicks -> {
                    double heading = -orientation.angles.y;
                    if (ConfigSystem.client.controlSettings.north360.value)
                        heading += 180;
                    while (heading < 0)
                        heading += 360;
                    while (heading > 360)
                        heading -= 360;
                    return heading;
                }, false);
            case ("pitch"):
                return new ComputedVariable(this, variable, partialTicks -> orientation.angles.x, false);
            case ("roll"):
                return new ComputedVariable(this, variable, partialTicks -> orientation.angles.z, false);
            case ("altitude"):
                return new ComputedVariable(this, variable, partialTicks -> position.y - seaLevel, false);
            case ("speed"):
                return new ComputedVariable(this, variable, partialTicks -> indicatedSpeed, false);
            case ("speed_scaled"):
                return new ComputedVariable(this, variable, partialTicks -> indicatedSpeed / speedFactor, false);
            case ("velocity"):
                return new ComputedVariable(this, variable, partialTicks -> velocity * speedFactor * 20F, false);
            case ("velocity_scaled"):
                return new ComputedVariable(this, variable, partialTicks -> velocity * 20F, false);
            case ("speed_factor"):
                return new ComputedVariable(this, variable, partialTicks -> speedFactor, false);
            case ("acceleration"):
                return new ComputedVariable(this, variable, partialTicks -> motion.length() - prevMotion.length(), false);
            case ("road_angle_front"):
                return new ComputedVariable(this, variable, partialTicks -> frontFollower != null ? frontFollower.getCurrentYaw() - orientation.angles.y : 0, false);
            case ("road_angle_rear"):
                return new ComputedVariable(this, variable, partialTicks -> rearFollower != null ? rearFollower.getCurrentYaw() - orientation.angles.y : 0, false);

            //Vehicle state cases.
            case("autopilot_present"):
                return new ComputedVariable(this, variable, partialTicks -> definition.motorized.hasAutopilot ? 1 : 0, false);
            case ("fuel"):
                return new ComputedVariable(this, variable, partialTicks -> fuelTank.getFluidLevel() / fuelTank.getMaxLevel(), false);
            case ("mass"):
                return new ComputedVariable(this, variable, partialTicks -> currentMass, false);
            case ("electric_power"):
                return new ComputedVariable(this, variable, partialTicks -> electricPower, false);
            case ("electric_usage"):
                return new ComputedVariable(this, variable, partialTicks -> electricFlow * 20D, false);
            case ("engines_on"):
                return new ComputedVariable(this, variable, partialTicks -> enginesOn ? 1 : 0, false);
            case ("engines_starting"):
                return new ComputedVariable(this, variable, partialTicks -> enginesStarting ? 1 : 0, false);
            case ("engines_running"):
                return new ComputedVariable(this, variable, partialTicks -> enginesRunning ? 1 : 0, false);
            case ("reverser"):
                return new ComputedVariable(this, variable, partialTicks -> reverseThrustVar.isActive ? 1 : 0, false);
            case ("reverser_present"):
                return new ComputedVariable(this, variable, partialTicks -> hasReverseThrust ? 1 : 0, false);
            case ("locked"):
                return new ComputedVariable(this, variable, partialTicks -> lockedVar.isActive ? 1 : 0, false);
            case ("door"):
                return new ComputedVariable(this, variable, partialTicks -> parkingBrakeVar.isActive && velocity < 0.25 ? 1 : 0, false);
            case ("fueling"):
                return new ComputedVariable(this, variable, partialTicks -> beingFueled ? 1 : 0, false);
            case ("thrust"):
                return new ComputedVariable(this, variable, partialTicks -> thrustForceValue, false);
            case ("vertical_acceleration"):
                return new ComputedVariable(this, variable, partialTicks -> -((Math.toRadians(rotation.angles.x) * 20F) * indicatedSpeed), false);
            case ("lateral_acceleration"):
                return new ComputedVariable(this, variable, partialTicks -> -((Math.toRadians(rotation.angles.y) * 20F) * indicatedSpeed), false);
            case ("vertical_acceleration_scaled"):
                return new ComputedVariable(this, variable, partialTicks -> -((Math.toRadians(rotation.angles.x) * 20F) * (indicatedSpeed / speedFactor)), false);
            case ("lateral_acceleration_scaled"):
                return new ComputedVariable(this, variable, partialTicks -> -((Math.toRadians(rotation.angles.y) * 20F) * (indicatedSpeed / speedFactor)), false);
            case ("load_factor"):
                return new ComputedVariable(this, variable, partialTicks -> (((Math.toRadians(-rotation.angles.x) * 20F) * indicatedSpeed) + 9.8) / 9.8, false);
            case ("load_factor_scaled"):
                return new ComputedVariable(this, variable, partialTicks -> (((Math.toRadians(-rotation.angles.x) * 20F) * (indicatedSpeed / speedFactor)) + 9.8) / 9.8, false);
            //State cases generally used on aircraft.
            case ("flaps_moving"):
                return new ComputedVariable(this, variable, partialTicks -> flapActualAngleVar.currentValue != flapDesiredAngleVar.currentValue ? 1 : 0, false);
            case ("flaps_increasing"):
                return new ComputedVariable(this, variable, partialTicks -> flapActualAngleVar.currentValue < flapDesiredAngleVar.currentValue ? 1 : 0, false);
            case ("flaps_decreasing"):
                return new ComputedVariable(this, variable, partialTicks -> flapActualAngleVar.currentValue > flapDesiredAngleVar.currentValue ? 1 : 0, false);
            case ("vertical_speed"):
                return new ComputedVariable(this, variable, partialTicks -> motion.y * speedFactor * 20, false);
            case ("lift_reserve"):
                return new ComputedVariable(this, variable, partialTicks -> -trackAngle, false);
            case ("turn_coordinator"):
                return new ComputedVariable(this, variable, partialTicks -> ((rotation.angles.z) / 10 + rotation.angles.y) / 0.15D * 25, false);
            case ("turn_indicator"):
                return new ComputedVariable(this, variable, partialTicks -> (rotation.angles.y) / 0.15F * 25F, false);
            case ("pitch_indicator"):
                return new ComputedVariable(this, variable, partialTicks -> (rotation.angles.x) / 0.15F * 25F, false);
            case ("slip"):
                return new ComputedVariable(this, variable, partialTicks -> 75 * sideVector.dotProduct(normalizedVelocityVector, true), false);
            case ("slip_degrees"):
                return new ComputedVariable(this, variable, partialTicks -> -Math.toDegrees(Math.asin(sideVector.dotProduct(normalizedVelocityVector, false))), false);
            case ("slip_understeer"):
                return new ComputedVariable(this, variable, partialTicks -> getSteeringAngle() * (1 - Math.max(0, Math.min(1, Math.abs(turningForce) / 10))), false);
            case ("gear_present"):
                return new ComputedVariable(this, variable, partialTicks -> definition.motorized.gearSequenceDuration != 0 ? 1 : 0, false);
            case ("gear_moving"):
                return new ComputedVariable(this, variable, partialTicks -> (retractGearVar.isActive ? gearMovementTime != definition.motorized.gearSequenceDuration : gearMovementTime != 0) ? 1 : 0, false);
            case ("beacon_direction"):
                return new ComputedVariable(this, variable, partialTicks -> selectedBeacon != null ? orientation.angles.getClampedYDelta(Math.toDegrees(Math.atan2(selectedBeacon.position.x - position.x, selectedBeacon.position.z - position.z))) : 0, false);
            case ("beacon_bearing_setpoint"):
                return new ComputedVariable(this, variable, partialTicks -> selectedBeacon != null ? selectedBeacon.bearing : 0, false);
            case ("beacon_bearing_delta"):
                return new ComputedVariable(this, variable, partialTicks -> selectedBeacon != null ? selectedBeacon.getBearingDelta(this) : 0, false);
            case ("beacon_glideslope_setpoint"):
                return new ComputedVariable(this, variable, partialTicks -> selectedBeacon != null ? selectedBeacon.glideSlope : 0, false);
            case ("beacon_glideslope_actual"):
                return new ComputedVariable(this, variable, partialTicks -> selectedBeacon != null ? Math.toDegrees(Math.asin((position.y - selectedBeacon.position.y) / position.distanceTo(selectedBeacon.position))) : 0, false);
            case ("beacon_glideslope_delta"):
                return new ComputedVariable(this, variable, partialTicks -> selectedBeacon != null ? selectedBeacon.glideSlope - Math.toDegrees(Math.asin((position.y - selectedBeacon.position.y) / position.distanceTo(selectedBeacon.position))) : 0, false);
            case ("beacon_distance"):
                return new ComputedVariable(this, variable, partialTicks -> selectedBeacon != null ? Math.hypot(-selectedBeacon.position.z + position.z,-selectedBeacon.position.x + position.x) : 0, false);
            case ("radar_detected"):
                return new ComputedVariable(this, variable, partialTicks -> radarsTracking.isEmpty() ? 0 : 1, false);
            case ("missile_incoming"):
                return new ComputedVariable(this, variable, partialTicks -> missilesIncoming.isEmpty() ? 0 : 1, false);
            default: {
                //Missile incoming variables.
                //Variable is in the form of missile_X_variablename.
                if (variable.startsWith("missile_")) {
                    final String missileVariable = variable.substring(variable.lastIndexOf("_") + 1);
                    final int missileNumber = ComputedVariable.getVariableNumber(variable.substring(0, variable.lastIndexOf('_')));
                    return new ComputedVariable(this, variable, partialTicks -> {
                        if (missilesIncoming.size() > missileNumber) {
                            switch (missileVariable) {
                                case ("distance"):
                                    return missilesIncoming.get(missileNumber).targetDistance;
                                case ("direction"): {
                                    Point3D missilePos = missilesIncoming.get(missileNumber).position;
                                    return Math.toDegrees(Math.atan2(-missilePos.z + position.z, -missilePos.x + position.x)) + 90 + orientation.angles.y;
                                }
                            }
                        }
                        return 0;
                    }, false);
                }else if (variable.startsWith("radar_")) {
                    final String[] parsedVariable = variable.split("_");
            
                    //First check if we are seeing with our own radar, or being seen.
                    //Variable is in the form of radar_X_variablename for inbound, radar_T_X_variablename for outbound.
                    if(parsedVariable.length == 3) {
                      //Inbound contact from another radar.
                        final int radarNumber = Integer.parseInt(parsedVariable[1]) - 1;
                        switch (parsedVariable[2]) {
                            case ("detected"):
                                return new ComputedVariable(this, variable, partialTicks -> radarsTracking.size() > radarNumber ? 1 : 0, false);
                            case ("distance"):
                                return new ComputedVariable(this, variable, partialTicks -> radarsTracking.size() > radarNumber ? radarsTracking.get(radarNumber).position.distanceTo(position) : 0, false);
                            case ("direction"): {
                                return new ComputedVariable(this, variable, partialTicks -> {
                                    if(radarsTracking.size() > radarNumber) {
                                        Point3D entityPos = radarsTracking.get(radarNumber).position;
                                        return Math.toDegrees(Math.atan2(-entityPos.z + position.z, -entityPos.x + position.x)) + 90 + orientation.angles.y;   
                                    }else {
                                        return 0;
                                    }
                                }, false);
                            }
                        }
                    }else if(parsedVariable.length == 4) {
                        //Outbound radar found, do logic.
                        final List<EntityVehicleF_Physics> radarList;
                        switch (parsedVariable[1]) {
                            case ("aircraft"): {
                                radarList = aircraftOnRadar;
                                break;
                            }
                            case ("ground"): {
                                radarList = groundersOnRadar;
                                break;
                            }
                            default:
                                //Invalid radar type.
                                return new ComputedVariable(false);
                        }
                        
                        final int radarNumber = Integer.parseInt(parsedVariable[2]) - 1;
                        switch (parsedVariable[3]) {
                            case ("distance"):
                                return new ComputedVariable(this, variable, partialTicks -> {
                                    if (radarNumber < radarList.size()) {
                                        AEntityB_Existing contact = radarList.get(radarNumber);
                                        return contact.position.distanceTo(position);
                                    } else {
                                        return 0;
                                    }
                                }, false);
                            case ("direction"):
                                return new ComputedVariable(this, variable, partialTicks -> {
                                    if (radarNumber < radarList.size()) {
                                        AEntityB_Existing contact = radarList.get(radarNumber);
                                        double delta = Math.toDegrees(Math.atan2(-contact.position.z + position.z, -contact.position.x + position.x)) + 90 + orientation.angles.y;
                                        while (delta < -180)
                                            delta += 360;
                                        while (delta > 180)
                                            delta -= 360;
                                        return delta;
                                    } else {
                                        return 0;
                                    }
                                }, false);
                            case ("speed"):
                                return new ComputedVariable(this, variable, partialTicks -> {
                                    if (radarNumber < radarList.size()) {
                                        AEntityB_Existing contact = radarList.get(radarNumber);
                                        return contact.velocity;
                                    } else {
                                        return 0;
                                    }
                                }, false);
                            case ("altitude"):
                                return new ComputedVariable(this, variable, partialTicks -> {
                                    if (radarNumber < radarList.size()) {
                                        AEntityB_Existing contact = radarList.get(radarNumber);
                                        return contact.position.y;
                                    } else {
                                        return 0;
                                    }
                                }, false);
                            case ("angle"):
                                return new ComputedVariable(this, variable, partialTicks -> {
                                    if (radarNumber < radarList.size()) {
                                        AEntityB_Existing contact = radarList.get(radarNumber);
                                        return -Math.toDegrees(Math.atan2(-contact.position.y + position.y, Math.hypot(-contact.position.z + position.z, -contact.position.x + position.x))) + orientation.angles.x;
                                    } else {
                                        return 0;
                                    }
                                }, false);
                        }
                    }
                    
                    //Down here means bad variable format.
                    return new ComputedVariable(false);
                }else {
                    return super.createComputedVariable(variable, createDefaultIfNotPresent);
                }
            }
        }
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
}
