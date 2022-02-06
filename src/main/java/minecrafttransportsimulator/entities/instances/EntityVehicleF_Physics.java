package minecrafttransportsimulator.entities.instances;

import java.util.HashSet;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.TrailerConnection;
import minecrafttransportsimulator.jsondefs.JSONVariableModifier;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.rendering.instances.RenderVehicle;
import minecrafttransportsimulator.systems.ConfigSystem;

/**This class adds the final layer of physics calculations on top of the
 * existing entity calculations.  Various control surfaces are present, as
 * well as helper functions and logic for controlling those surfaces.
 * Note that angle variables here should be divided by 10 to get actual angle.
 * 
 * @author don_bruce
 */
public class EntityVehicleF_Physics extends AEntityVehicleE_Powered{
	//Aileron.
	@DerivedValue
	public double aileronAngle;
	@DerivedValue
	public double aileronTrim;
	public static final double MAX_AILERON_ANGLE = 25;
	public static final double MAX_AILERON_TRIM = 10;
	public static final double AILERON_DAMPEN_RATE = 0.6;
	public static final String AILERON_VARIABLE = "aileron";
	public static final String AILERON_TRIM_VARIABLE = "trim_aileron";
	
	//Elevator.
	@DerivedValue
	public double elevatorAngle;
	@DerivedValue
	public double elevatorTrim;
	public static final double MAX_ELEVATOR_ANGLE = 25;
	public static final double MAX_ELEVATOR_TRIM = 10;
	public static final double ELEVATOR_DAMPEN_RATE = 0.6;
	public static final String ELEVATOR_VARIABLE = "elevator";
	public static final String ELEVATOR_TRIM_VARIABLE = "trim_elevator";
	
	//Rudder.
	@DerivedValue
	public double rudderAngle;
	@DerivedValue
	public double rudderTrim;
	public static final double MAX_RUDDER_ANGLE = 45;
	public static final double MAX_RUDDER_TRIM = 10;
	public static final double RUDDER_DAMPEN_RATE = 2.0;
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
	public static final String AUTOPILOT_VARIABLE = "autopilot";
	public static final String AUTOLEVEL_VARIABLE = "auto_level";
	
	//Internal states.
	public boolean hasRotors;
	private double pitchDirectionFactor;
	public double trackAngle;
	private final Set<EntityVehicleF_Physics> towedVehiclesCheckedForWeights = new HashSet<EntityVehicleF_Physics>();
	
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
	private Point3d thrustForce = new Point3d();//kg*m/ticks^2
	private Point3d totalAxialForce = new Point3d();//kg*m/ticks^2
	private Point3d totalMotiveForce = new Point3d();//kg*m/ticks^2
	private Point3d totalGlobalForce = new Point3d();//kg*m/ticks^2
	private Point3d totalForce = new Point3d();//kg*m/ticks^2
	
	//Torques.
	private double momentRoll;//kg*m^2
	private double momentPitch;//kg*m^2
	private double momentYaw;//kg*m^2
	private double aileronTorque;//kg*m^2/ticks^2
	private double elevatorTorque;//kg*m^2/ticks^2
	private double rudderTorque;//kg*m^2/ticks^2
	private Point3d thrustTorque = new Point3d();//kg*m^2/ticks^2
	private Point3d totalTorque = new Point3d();//kg*m^2/ticks^2
	private Point3d rotorRotation = new Point3d();//degrees
	
	//Animator for vehicles
	private static RenderVehicle renderer;;

	public EntityVehicleF_Physics(WrapperWorld world, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, placingPlayer, data);
		this.flapCurrentAngle = data.getDouble("flapCurrentAngle");
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			world.beginProfiling("VehicleF_Level", true);
			//Parse out variables.
			aileronAngle = getVariable(AILERON_VARIABLE);
			aileronTrim = getVariable(AILERON_TRIM_VARIABLE);
			elevatorAngle = getVariable(ELEVATOR_VARIABLE);
			elevatorTrim = getVariable(ELEVATOR_TRIM_VARIABLE);
			rudderAngle = getVariable(RUDDER_VARIABLE);
			rudderTrim = getVariable(RUDDER_TRIM_VARIABLE);
			autopilotSetting = getVariable(AUTOPILOT_VARIABLE);
			flapDesiredAngle = getVariable(FLAPS_VARIABLE);
			
			//Adjust flaps to current setting.
			if(definition.motorized.flapNotches != null && !definition.motorized.flapNotches.isEmpty()){
				if(flapCurrentAngle < flapDesiredAngle){
					flapCurrentAngle += definition.motorized.flapSpeed;
				}else if(flapCurrentAngle > flapDesiredAngle){
					flapCurrentAngle -= definition.motorized.flapSpeed;
				}
				if(Math.abs(flapCurrentAngle - flapDesiredAngle) < definition.motorized.flapSpeed){
					flapCurrentAngle = flapDesiredAngle;
				}
			}
			world.endProfiling();
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public double getMass(){
		//Need to use a list here to make sure we don't end up with infinite recursion due to bad trailer linkings.
		//This could lock up a world if not detected!
		double combinedMass = super.getMass();
		if(!towingConnections.isEmpty()){
			EntityVehicleF_Physics otherVehicle = null;
			for(TrailerConnection connection : towingConnections){
				//Only check once per base entity.
				otherVehicle = connection.hookupVehicle;
				if(towedVehiclesCheckedForWeights.contains(otherVehicle)){
					InterfaceCore.logError("Infinite loop detected on weight checking code!  Is a trailer towing the thing that's towing it?");
					break;
				}else{
					towedVehiclesCheckedForWeights.add(otherVehicle);
					combinedMass += otherVehicle.getMass();
					otherVehicle = null;
					towedVehiclesCheckedForWeights.clear();
				}
			}
			//If we still have a vehicle reference, we didn't exit cleanly and need to disconnect it.
			if(otherVehicle != null){
				disconnectTrailer(otherVehicle.towedByConnection);
			}
		}
		return combinedMass;
	}
	
	@Override
	protected double getSteeringAngle(){
		return -rudderAngle/(float)MAX_RUDDER_ANGLE;
	}
	
	@Override
	protected void addToSteeringAngle(float degrees){
		//Invert the degrees, as rudder is inverted from normal steering.
		double delta = 0;
		if(rudderAngle - degrees > MAX_RUDDER_ANGLE){
			delta = MAX_RUDDER_ANGLE - rudderAngle;
		}else if(rudderAngle - degrees < -MAX_RUDDER_ANGLE){
			delta = -MAX_RUDDER_ANGLE - rudderAngle;
		}else{
			delta = -degrees;
		}
		setVariable(RUDDER_VARIABLE, rudderAngle + delta);
		InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, RUDDER_VARIABLE, delta));
	}
	
	@Override
	protected void updateVariableModifiers(){
		currentWingArea = (float) (definition.motorized.wingArea + definition.motorized.wingArea*0.15F*flapCurrentAngle/MAX_FLAP_ANGLE_REFERENCE);
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
		
		//Adjust current variables to modifiers, if any exist.
		if(definition.variableModifiers != null){
			for(JSONVariableModifier modifier : definition.variableModifiers){
				switch(modifier.variable){
					case "wingArea" : currentWingArea = adjustVariable(modifier, currentWingArea); break;
					case "wingSpan" : currentWingSpan = adjustVariable(modifier, currentWingSpan); break;
					case "aileronArea" : currentAileronArea = adjustVariable(modifier, currentAileronArea); break;
					case "elevatorArea" : currentElevatorArea = adjustVariable(modifier, currentElevatorArea); break;
					case "rudderArea" : currentRudderArea = adjustVariable(modifier, currentRudderArea); break;
					case "dragCoefficient" : currentDragCoefficient = adjustVariable(modifier, currentDragCoefficient); break;
					case "ballastVolume" : currentBallastVolume = adjustVariable(modifier, currentBallastVolume); break;
					case "downForce" : currentDownForce = adjustVariable(modifier, currentDownForce); break;
					case "brakingFactor" : currentBrakingFactor = adjustVariable(modifier, currentBrakingFactor); break;
					case "overSteer" : currentOverSteer = adjustVariable(modifier, currentOverSteer); break;
					case "underSteer" : currentUnderSteer = adjustVariable(modifier, currentUnderSteer); break;
					case "axleRatio" : currentAxleRatio = adjustVariable(modifier, currentAxleRatio); break;
					default : setVariable(modifier.variable, adjustVariable(modifier, (float) getVariable(modifier.variable))); break;
				}
			}
		}
	}
	
	@Override
	protected void getForcesAndMotions(){
		//If we are free, do normal updates.  But if we are towed by a vehicle, do trailer forces instead.
		//This prevents trailers from behaving badly and flinging themselves into the abyss.
		if(towedByConnection == null){
			//Set moments.
			momentRoll = definition.motorized.emptyMass*(1.5F + fuelTank.getFluidLevel()/10000F);
			momentPitch = 2D*currentMass;
			momentYaw = 3D*currentMass;
			
			//Get engine thrust force contributions.
			thrustForce.set(0D, 0D, 0D);
			thrustTorque.set(0D, 0D, 0D);
			rotorRotation.set(0D, 0D, 0D);
			for(APart part : parts){
				Point3d partForce;
				boolean isPropeller = false;
				boolean isRotor = false;
				double jetPower = 0;
				if(part instanceof PartEngine){
					partForce = ((PartEngine) part).getForceOutput();
					jetPower = part.definition.engine.jetPowerFactor;
				}else if(part instanceof PartPropeller){
					partForce = ((PartPropeller) part).getForceOutput();
					isPropeller = true;
					isRotor = part.definition.propeller.isRotor;
				}else{
					continue;
				}
				
				thrustForce.add(partForce);
				
				//If the part is a propeller or jet engine (not a car engine), we add thrust torque.
				//If it's a rotor, we also add control surface torque to allow the vehicle to tilt.
				if(isPropeller || jetPower > 0){
					thrustTorque.add(partForce.y*-part.localOffset.z, partForce.z*part.localOffset.x, partForce.y*part.localOffset.x);
				}
				if(isRotor && !groundDeviceCollective.isAnythingOnGround() && partForce.length() > 1){
					hasRotors = true;
					if(getVariable(AUTOLEVEL_VARIABLE) != 0){
						rotorRotation.set((-(elevatorAngle + elevatorTrim) - angles.x)/MAX_ELEVATOR_ANGLE, -5D*rudderAngle/MAX_RUDDER_ANGLE, ((aileronAngle + aileronTrim) - angles.z)/MAX_AILERON_ANGLE);
					}else{
						if(autopilotSetting == 0){
							rotorRotation.add(-5D*elevatorAngle/MAX_ELEVATOR_ANGLE, -5D*rudderAngle/MAX_RUDDER_ANGLE, 5D*aileronAngle/MAX_AILERON_ANGLE);
						}else{
							if(angles.x < -1){
								rotorRotation.x = 1;
							}else if(angles.x > 1){
								rotorRotation.x = -1;
							}else{
								rotorRotation.x = -angles.x;
							}
							if(angles.z < -1){
								rotorRotation.z = 1;
							}else if(angles.z > 1){
								rotorRotation.z = -1;
							}else{
								rotorRotation.z = -angles.z;
							}
							rotorRotation.y = -5D*rudderAngle/MAX_RUDDER_ANGLE;
						}
					}
				}else{
					rotorRotation.set(0, 0, 0);
				}
			}
			
			//Get forces.  Some forces are specific to JSON sections.
			//First get gravity.
			gravitationalForce = currentBallastVolume == 0 ? currentMass*(9.8/400) : 0;
			if(!definition.motorized.isAircraft){
				gravitationalForce *= ConfigSystem.configObject.general.gravityFactor.value;
			}
			
			//Get the track angle.  This is used for control surfaces.
			trackAngle = -Math.toDegrees(Math.asin(verticalVector.dotProduct(normalizedVelocityVector)));
			
			//Set blimp-specific states before calculating forces.
			if(definition.motorized.isBlimp){
				//Blimps are turned with rudders, not ailerons.  This puts the keys at an odd location.  To compensate, 
				//we set the rudder to the aileron if the aileron is greater or less than the rudder.  That way no matter 
				//which key is pressed, they both activate the rudder for turning.
				if((aileronAngle < 0 && aileronAngle < rudderAngle) || (aileronAngle > 0 && aileronAngle > rudderAngle)){
					rudderAngle = aileronAngle;
				}
				
				//If we have the brake pressed at a slow speed, stop the blimp.
				//This is needed to prevent runaway blimps.
				if(Math.abs(velocity) < 0.15 && (brake > 0 || parkingBrakeOn)){
					motion.x = 0;
					motion.z = 0;
					thrustForce.set(0D, 0D, 0D);
					thrustTorque.set(0D, 0D, 0D);
				}
			}
			
			//Get the lift coefficients and states for control surfaces.
			double yawAngleDelta = Math.toDegrees(Math.asin(sideVector.dotProduct(normalizedVelocityVector)));
			wingLiftCoeff = getLiftCoeff(trackAngle, 2 + flapCurrentAngle/MAX_FLAP_ANGLE_REFERENCE);
			aileronLiftCoeff = getLiftCoeff((aileronAngle + aileronTrim), 2);
			elevatorLiftCoeff = getLiftCoeff(-2.5 + trackAngle - (elevatorAngle + elevatorTrim), 2);
			rudderLiftCoeff = getLiftCoeff((rudderAngle + rudderTrim) - yawAngleDelta, 2);
			
			//Get the drag coefficient and force.
			if(definition.motorized.isBlimp){
				dragCoeff = 0.004F*Math.pow(Math.abs(yawAngleDelta), 2) + currentDragCoefficient;
			}else if(definition.motorized.isAircraft){
				//Aircraft are 0.03 by default, or whatever is specified.
				dragCoeff = 0.0004F*Math.pow(trackAngle, 2) + currentDragCoefficient;
			}else{
				dragCoeff = currentDragCoefficient;
				//If we aren't an aircraft, check for grounded ground devices.
				//If we don't have any grounded ground devices, assume we are in the air or in water.
				//This results in an increase in drag due to poor airflow.
				if(groundDeviceCollective.groundedGroundDevices.isEmpty()){
					dragCoeff *= 3D;
				}
			}
			if(definition.motorized.crossSectionalArea > 0){
				dragForce = 0.5F*airDensity*velocity*velocity*definition.motorized.crossSectionalArea*dragCoeff;
			}else if(currentWingSpan > 0){
				dragForce = 0.5F*airDensity*velocity*velocity*currentWingArea*(dragCoeff + wingLiftCoeff*wingLiftCoeff/(Math.PI*definition.motorized.wingSpan*definition.motorized.wingSpan/currentWingArea*0.8));
			}else{
				dragForce = 0.5F*airDensity*velocity*velocity*5.0F*dragCoeff;
			}
			
			//Get ballast force.
			if(currentBallastVolume > 0){
				//Ballast gets less effective at applying positive lift at higher altitudes.
				//This prevents blimps from ascending into space.
				//Also take into account motionY, as we should provide less force if we are already going in the same direction.
				if(elevatorAngle < 0){
					ballastForce = airDensity*currentBallastVolume*-elevatorAngle/10D;
				}else if(elevatorAngle > 0){
					ballastForce = 1.225*currentBallastVolume*-elevatorAngle/10D;
				}else{
					ballastForce = 1.225*currentBallastVolume*10D*-motion.y;
				}
				if(motion.y*ballastForce != 0){
					ballastForce /= Math.pow(1 + Math.abs(motion.y), 2);
				}
			}
			
			//Get all other forces.
			wingForce = 0.5F*airDensity*axialVelocity*axialVelocity*currentWingArea*wingLiftCoeff;
			aileronForce = 0.5F*airDensity*axialVelocity*axialVelocity*currentAileronArea*aileronLiftCoeff;
			elevatorForce = 0.5F*airDensity*axialVelocity*axialVelocity*currentElevatorArea*elevatorLiftCoeff;			
			rudderForce = 0.5F*airDensity*axialVelocity*axialVelocity*currentRudderArea*rudderLiftCoeff;
			
			//Get torques.  Point for ailerons is 0.75% to the edge of the wing.
			aileronTorque = aileronForce*currentWingSpan*0.5F*0.75F;
			elevatorTorque = elevatorForce*definition.motorized.tailDistance;
			rudderTorque = rudderForce*definition.motorized.tailDistance;
			
			//If the elevator torque is low, don't apply it.  This prevents elevators from
			//having effects at slow speeds.  We use a faux-torque here from the main plane
			//body to check if we are below this point.
			if(Math.abs(elevatorTorque) < 2D*currentMass/400D){
				elevatorTorque = 0;
			}
			
			//Do more blimp-specific things for the forces.
			if(definition.motorized.isBlimp){
				//Roll and pitch are applied only if we aren't level.
				//This only happens if we fall out of the sky and land on the ground and tilt.
				if(angles.z > 0){
					aileronTorque = -Math.min(0.5F, angles.z)*currentMass/100;
				}else if(angles.z < 0){
					aileronTorque = -Math.max(-0.5F, angles.z)*currentMass/100;
				}else{
					aileronTorque = 0;
				}
				if(angles.x > 0){
					elevatorTorque = -Math.min(0.5F, angles.x)*currentMass/100;
				}else if(angles.x < 0){
					elevatorTorque = -Math.max(-0.5F, angles.x)*currentMass/100;
				}else{
					elevatorTorque = 0;
				}
				
				//If we are turning with the rudder, don't let us heel out of line easily.
				//Rudder force should be minimal for blimps due to their moment of inertia.
				if(rudderTorque*rudderAngle <= 0){
					rudderTorque = 0;
				}
			}
			
			//As a special case, if the vehicle is a stalled plane, add a forwards pitch to allow the plane to right itself.
			//This is needed to prevent the plane from getting stuck in a vertical position and crashing.
			if(currentWingArea > 0 && trackAngle > 40 && angles.x < 45 && !groundDeviceCollective.isAnythingOnGround()){
				elevatorTorque += 100;
			}
			
			//If we are damaged, don't apply control surface and ballast forces.
			if(damageAmount == definition.general.health){
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
			totalAxialForce.set(0D, wingForce - elevatorForce, 0D).add(thrustForce).rotateFine(angles);
			totalMotiveForce.set(-dragForce, -dragForce, -dragForce).multiply(normalizedVelocityVector);
			totalGlobalForce.set(0D, ballastForce - gravitationalForce, 0D);
			totalForce.setTo(totalAxialForce).add(totalMotiveForce).add(totalGlobalForce).multiply(1/currentMass);
			motion.add(totalForce);
			
			//Add all torques to the main torque matrix and apply them.
			pitchDirectionFactor = Math.abs(angles.z%360);
			pitchDirectionFactor = pitchDirectionFactor < 90 || pitchDirectionFactor > 270 ? 1.0D : -1.0D;
			totalTorque.set(elevatorTorque, rudderTorque, aileronTorque).add(thrustTorque).multiply(180D/Math.PI);
			rotation.x = (pitchDirectionFactor*(1-Math.abs(sideVector.y))*totalTorque.x + sideVector.y*totalTorque.y)/momentPitch;
			rotation.y = (sideVector.y*totalTorque.x - verticalVector.y*totalTorque.y)/momentYaw;
			rotation.z = totalTorque.z/momentRoll;
			rotation.add(rotorRotation);
		}else if(!lockedOnRoad){
			//If we are a trailer that is mounted, just move the vehicle to the exact position of the trailer connection.
			//Otherwise, do movement logic  Make sure the towed vehicle is loaded, however.  It may not yet be.
			if(towedByConnection.hitchConnection.mounted){
				Point3d hitchRotatedOffset = towedByConnection.getHitchCurrentPosition();
				Point3d hookupRotatedOffset = towedByConnection.getHookupCurrentPosition();
				motion.setTo(hitchRotatedOffset).subtract(hookupRotatedOffset).multiply(1/SPEED_FACTOR);
				//TODO whatever maths we apply to the part rendering we need to apply here.
				rotation.setTo(towedByConnection.hitchEntity.angles).add(towedByConnection.hitchConnection.rot).subtract(angles);
			}else{
				//Need to apply both motion to move the trailer, and yaw to adjust the trailer's angle relative to the truck.
				//Yaw is applied based on the current and next position of the truck's hookup.
				//Motion is applied after yaw corrections to ensure the trailer follows the truck.
				//Start by getting the hitch offsets.  We save the current offset as we'll change it for angle calculations.
				Point3d tractorHitchPrevOffsetXZ = towedByConnection.getHitchPrevPosition().subtract(prevPosition);
				Point3d tractorHitchCurrentOffsetXZ = towedByConnection.getHitchCurrentPosition().subtract(position);
				Point3d tractorHitchCurrentOffset = tractorHitchCurrentOffsetXZ.copy();
				
				//Calculate how much yaw we need to apply to rotate the trailer.
				//This is only done for the X and Z motions.
				//If we are restricted, make yaw match the hookup.
				tractorHitchPrevOffsetXZ.y = 0;
				tractorHitchCurrentOffsetXZ.y = 0;
				tractorHitchPrevOffsetXZ.normalize();
				tractorHitchCurrentOffsetXZ.normalize();
				double rotationDelta;
				if(towedByConnection.hitchConnection.restricted){
					rotationDelta = towedByConnection.hitchEntity.angles.y - angles.y;
				}else{
					rotationDelta = Math.toDegrees(Math.acos(tractorHitchPrevOffsetXZ.dotProduct(tractorHitchCurrentOffsetXZ)));
					rotationDelta *= Math.signum(tractorHitchPrevOffsetXZ.crossProduct(tractorHitchCurrentOffsetXZ).y);
				}
				
				//If the rotation is valid, add it.
				//We need to fake-add the yaw for the motion calculation here, hence the odd temp setting of the angles.
				Point3d trailerHookupOffset;
				if(!Double.isNaN(rotationDelta)){
					rotation.y = rotationDelta;
					angles.y += rotationDelta;
					trailerHookupOffset = towedByConnection.getHookupCurrentPosition().subtract(position);
					angles.y -= rotationDelta;
				}else{
					trailerHookupOffset = towedByConnection.getHookupCurrentPosition().subtract(position);
				}
				
				//Now move the trailer to the hitch.  Also set rotations to 0 to prevent odd math.
				motion.setTo(tractorHitchCurrentOffset.subtract(trailerHookupOffset).multiply(1/SPEED_FACTOR));
				rotation.x = 0;
				rotation.z = 0;
			}
		}else{
			motion.setTo(towedByConnection.hitchVehicle.motion);
			rotation.set(0, 0, 0);
		}
	}
	
	@Override
	protected void adjustControlSurfaces(){
		if(!definition.motorized.isAircraft && autopilotSetting != 0){
			//Car, do cruise control.
			if(velocity < autopilotSetting){
				if(throttle < MAX_THROTTLE){
					throttle += MAX_THROTTLE/100D;
					setVariable(THROTTLE_VARIABLE, throttle);
					InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, THROTTLE_VARIABLE, MAX_THROTTLE/100D));
				}
			}else if(velocity > autopilotSetting){
				if(throttle > 0){
					throttle -= MAX_THROTTLE/100D;
					setVariable(THROTTLE_VARIABLE, throttle);
					InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, THROTTLE_VARIABLE, -MAX_THROTTLE/100D));
				}
			}
		}
		
		if(hasRotors){
			//Helicopter.  Do auto-hover code if required.
			if(autopilotSetting != 0){
				//Change throttle to maintain altitude.
				//Only do this once every 1/2 second to allow for thrust changes.
				if(ticksExisted%10 == 0){
					if(motion.y < 0 && throttle < MAX_THROTTLE){
						throttle += MAX_THROTTLE/100D;
						setVariable(THROTTLE_VARIABLE, throttle);
						InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, THROTTLE_VARIABLE, MAX_THROTTLE/100D));
					}else if(motion.y > 0 && throttle < MAX_THROTTLE){
						throttle -= MAX_THROTTLE/100D;
						setVariable(THROTTLE_VARIABLE, throttle);
						InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, THROTTLE_VARIABLE, -MAX_THROTTLE/100D));
					}
				}
				//Change pitch/roll based on movement.
				double forwardsVelocity = motion.dotProduct(headingVector);
				double sidewaysVelocity = motion.dotProduct(sideVector);
				double forwardsDelta = forwardsVelocity - prevMotion.dotProduct(headingVector);
				double sidewaysDelta = sidewaysVelocity - prevMotion.dotProduct(sideVector);
				if(forwardsDelta > 0 && forwardsVelocity > 0 && elevatorTrim < MAX_ELEVATOR_TRIM){
					setVariable(ELEVATOR_TRIM_VARIABLE, elevatorTrim + 1);
					InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_TRIM_VARIABLE, 1));
				}else if(forwardsDelta < 0 && forwardsVelocity < 0 && elevatorTrim > -MAX_ELEVATOR_TRIM){
					setVariable(ELEVATOR_TRIM_VARIABLE, elevatorTrim - 1);
					InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_TRIM_VARIABLE, -1));
				}
				if(sidewaysVelocity > 0 && sidewaysDelta > 0 && aileronTrim < MAX_AILERON_TRIM){
					setVariable(AILERON_TRIM_VARIABLE, aileronTrim + 1);
					InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_TRIM_VARIABLE, 1));
				}else if(sidewaysVelocity < 0 && sidewaysDelta < 0  && aileronTrim > -MAX_AILERON_TRIM){
					setVariable(AILERON_TRIM_VARIABLE, aileronTrim - 1);
					InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_TRIM_VARIABLE, -1));
				}
			}else{
				//Reset trim to prevent directional surges.
				if(elevatorTrim < 0){
					setVariable(ELEVATOR_TRIM_VARIABLE, elevatorTrim + 1);
					InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_TRIM_VARIABLE, 1));
				}else if(elevatorTrim > 0){
					setVariable(ELEVATOR_TRIM_VARIABLE, elevatorTrim - 1);
					InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_TRIM_VARIABLE, -1));
				}
				if(aileronTrim < 0){
					setVariable(AILERON_TRIM_VARIABLE, aileronTrim + 1);
					InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_TRIM_VARIABLE, 1));
				}else if(aileronTrim > 0){
					setVariable(AILERON_TRIM_VARIABLE, aileronTrim - 1);
					InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_TRIM_VARIABLE, -1));
				}
			}
		}else if(definition.motorized.isAircraft && autopilotSetting != 0){
			//Normal aircraft.  Do autopilot operations if required.
			//If we are not flying at a steady elevation, angle the elevator to compensate
			if(-motion.y*100 > elevatorTrim + 1 && elevatorTrim < MAX_ELEVATOR_TRIM){
				setVariable(ELEVATOR_TRIM_VARIABLE, elevatorTrim + 0.1);
				InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_TRIM_VARIABLE, 0.1));
			}else if(-motion.y*100 < elevatorTrim - 1 && elevatorTrim > -MAX_ELEVATOR_TRIM){
				setVariable(ELEVATOR_TRIM_VARIABLE, elevatorTrim - 0.1);
				InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_TRIM_VARIABLE, -0.1));
			}
			//Keep the roll angle at 0.
			if(-angles.z > aileronTrim + 0.1 && aileronTrim < MAX_AILERON_TRIM){
				setVariable(AILERON_TRIM_VARIABLE, aileronTrim + 0.1);
				InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_TRIM_VARIABLE, 0.1));
			}else if(-angles.z < aileronTrim - 0.1 && aileronTrim > -MAX_AILERON_TRIM){
				setVariable(AILERON_TRIM_VARIABLE, aileronTrim - 0.1);
				InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_TRIM_VARIABLE, -0.1));
			}
		}
		
		//If we don't have a controller, reset control states to 0.
		boolean haveController = false;
		for(Point3d partPos : locationRiderMap.keySet()){
			APart part = getPartAtLocation(partPos);
			if(part instanceof PartSeat){
				if(part.placementDefinition.isController){
					haveController = true;
					break;
				}
			}
		}
		
		if(!haveController && !lockedOnRoad){
			if(aileronAngle > AILERON_DAMPEN_RATE){
				setVariable(AILERON_VARIABLE, aileronAngle - AILERON_DAMPEN_RATE);
				InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_VARIABLE, -AILERON_DAMPEN_RATE, 0, MAX_AILERON_ANGLE));
			}else if(aileronAngle < -AILERON_DAMPEN_RATE){
				setVariable(AILERON_VARIABLE, aileronAngle + AILERON_DAMPEN_RATE);
				InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, AILERON_VARIABLE, AILERON_DAMPEN_RATE, -MAX_AILERON_ANGLE, 0));
			}else if(aileronAngle != 0){
				setVariable(AILERON_VARIABLE, 0);
				InterfacePacket.sendToAllClients(new PacketEntityVariableSet(this, AILERON_VARIABLE, 0));
			}
			
			if(elevatorAngle > ELEVATOR_DAMPEN_RATE){
				setVariable(ELEVATOR_VARIABLE, elevatorAngle - ELEVATOR_DAMPEN_RATE);
				InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_VARIABLE, -ELEVATOR_DAMPEN_RATE, 0, MAX_ELEVATOR_ANGLE));
			}else if(elevatorAngle < -ELEVATOR_DAMPEN_RATE){
				setVariable(ELEVATOR_VARIABLE, elevatorAngle + ELEVATOR_DAMPEN_RATE);
				InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, ELEVATOR_VARIABLE, ELEVATOR_DAMPEN_RATE, -MAX_ELEVATOR_ANGLE, 0));
			}else if(elevatorAngle != 0){
				setVariable(ELEVATOR_VARIABLE, 0);
				InterfacePacket.sendToAllClients(new PacketEntityVariableSet(this, ELEVATOR_VARIABLE, 0));
			}
			
			if(rudderAngle > RUDDER_DAMPEN_RATE){
				setVariable(RUDDER_VARIABLE, rudderAngle - RUDDER_DAMPEN_RATE);
				InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, RUDDER_VARIABLE, -RUDDER_DAMPEN_RATE, 0, MAX_RUDDER_ANGLE));
			}else if(rudderAngle < -RUDDER_DAMPEN_RATE){
				setVariable(RUDDER_VARIABLE, rudderAngle + RUDDER_DAMPEN_RATE);
				InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, RUDDER_VARIABLE, RUDDER_DAMPEN_RATE, -MAX_RUDDER_ANGLE, 0));
			}else if(rudderAngle != 0){
				setVariable(RUDDER_VARIABLE, 0);
				InterfacePacket.sendToAllClients(new PacketEntityVariableSet(this, RUDDER_VARIABLE, 0));
			}
		}
	}
	
	protected static double getLiftCoeff(double angleOfAttack, double maxLiftCoeff){
		if(angleOfAttack == 0){
			return 0;
		}else if(Math.abs(angleOfAttack) <= 15*1.25){
			return maxLiftCoeff*Math.sin(Math.PI/2*angleOfAttack/15);
		}else if(Math.abs(angleOfAttack) <= 15*1.5){
			if(angleOfAttack > 0){
				return maxLiftCoeff*(0.4 + 1/(angleOfAttack - 15));
			}else{
				return maxLiftCoeff*(-0.4 + 1/(angleOfAttack + 15));
			}
		}else{
			return maxLiftCoeff*Math.sin(Math.PI/6*angleOfAttack/15);
		}
	}
	
	@Override
	public boolean shouldRenderBeams(){
    	return ConfigSystem.configObject.clientRendering.vehicleBeams.value;
    }
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		//If we are a forwarded variable and are a connected trailer, do that now.
		if(definition.motorized.isTrailer && towedByConnection != null && definition.motorized.hookupVariables.contains(variable)){
			return towedByConnection.hitchVehicle.getRawVariableValue(variable, partialTicks);
		}
		
		//Not a part of a forwarded variable.  Just return normally.
		switch(variable){
			//Vehicle world state cases.
			case("yaw"): return angles.y;
			case("heading"): int heading = (int)-angles.y; if(ConfigSystem.configObject.clientControls.north360.value) heading += 180; while (heading < 1) heading += 360; while (heading > 360) heading -= 360; return heading;
			case("pitch"): return angles.x;
			case("roll"): return angles.z;
			case("altitude"): return position.y;
			case("speed"): return axialVelocity*EntityVehicleF_Physics.SPEED_FACTOR*20;
			case("speed_scaled"): return axialVelocity*20;
			case("acceleration"): return motion.length() - prevMotion.length();

			//Vehicle state cases.
			case("fuel"): return fuelTank.getFluidLevel()/fuelTank.getMaxLevel();
			case("electric_power"): return electricPower;
			case("electric_usage"): return electricFlow*20D;
			case("engines_on"): return enginesOn ? 1 : 0;
			case("engines_running"): return enginesRunning ? 1 : 0;
			case("reverser"): return reverseThrust ? 1 : 0;
			case("locked"): return locked ? 1 : 0;
			case("door"): return parkingBrakeOn && velocity < 0.25 ? 1 : 0;
			case("fueling"): return beingFueled ? 1 : 0;
			
			//State cases generally used on aircraft.
			case("flaps_actual"): return flapCurrentAngle;
			case("flaps_moving"): return flapCurrentAngle != flapDesiredAngle ? 1 : 0;
			case("vertical_speed"): return motion.y*EntityVehicleF_Physics.SPEED_FACTOR*20;
			case("lift_reserve"): return -trackAngle;
			case("turn_coordinator"): return ((angles.z - prevAngles.z)/10 + angles.y - prevAngles.y)/0.15D*25;
			case("turn_indicator"): return (angles.y - prevAngles.y)/0.15F*25F;
			case("slip"): return 75*sideVector.dotProduct(normalizedVelocityVector);
			case("gear_moving"): return (isVariableActive(GEAR_VARIABLE) ? gearMovementTime != definition.motorized.gearSequenceDuration : gearMovementTime != 0) ? 1 : 0;
			case("beacon_direction"): return selectedBeacon != null ? angles.getClampedYDelta(Math.toDegrees(Math.atan2(selectedBeacon.position.x - position.x, selectedBeacon.position.z - position.z))) : 0;
			case("beacon_bearing_setpoint"): return selectedBeacon != null ? selectedBeacon.bearing : 0;
			case("beacon_bearing_delta"): return selectedBeacon != null ? selectedBeacon.getBearingDelta(this) : 0;
			case("beacon_glideslope_setpoint"): return selectedBeacon != null ? selectedBeacon.glideSlope : 0;
			case("beacon_glideslope_actual"): return selectedBeacon != null ? Math.toDegrees(Math.asin((position.y - selectedBeacon.position.y)/position.distanceTo(selectedBeacon.position))) : 0;
			case("beacon_glideslope_delta"): return selectedBeacon != null ? selectedBeacon.glideSlope - Math.toDegrees(Math.asin((position.y - selectedBeacon.position.y)/position.distanceTo(selectedBeacon.position))) : 0;
			
			default: {
				//Missile incoming variables.
				//Variable is in the form of missile_X_variablename.
				if(variable.startsWith("missile_")){
					String missileVariable = variable.substring(variable.lastIndexOf("_") + 1);
					int missileNumber = getVariableNumber(variable.substring(0, variable.lastIndexOf('_')));
					if(missileNumber != -1){
						if(missilesIncoming.size() <= missileNumber){
							return 0;
						}else{
							switch(missileVariable){
								case("distance"): return missilesIncoming.get(missileNumber).targetDistance;
								case("direction"): {
									Point3d missilePos = missilesIncoming.get(missileNumber).position;
									return Math.toDegrees(Math.atan2(-missilePos.z + position.z, -missilePos.x + position.x)) + 90 + angles.y;
								}
							}
						}
					}else if(missileVariable.equals("incoming")){
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
	@SuppressWarnings("unchecked")
	public RenderVehicle getRenderer(){
		if(renderer == null){
			renderer = new RenderVehicle();
		}
		return renderer;
	}
    
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setDouble("flapCurrentAngle", flapCurrentAngle);
		return data;
	}
}
