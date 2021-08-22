package minecrafttransportsimulator.entities.instances;

import java.util.HashSet;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.TrailerConnection;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
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
	public static final short MAX_AILERON_ANGLE = 250;
	public static final short MAX_AILERON_TRIM = 100;
	public static final short AILERON_DAMPEN_RATE = 6;
	public short aileronAngle;
	public short aileronTrim;
	public byte aileronCooldown;
	
	//Elevator.
	public static final short MAX_ELEVATOR_ANGLE = 250;
	public static final short MAX_ELEVATOR_TRIM = 100;
	public static final short ELEVATOR_DAMPEN_RATE = 6;
	public short elevatorAngle;
	public short elevatorTrim;
	public byte elevatorCooldown;
	
	//Rudder.
	public static final short MAX_RUDDER_ANGLE = 450;
	public static final short MAX_RUDDER_TRIM = 100;
	public static final short RUDDER_DAMPEN_RATE = 20;
	public short rudderAngle;
	public short rudderTrim;
	public byte rudderCooldown;
	
	//Flaps.
	public static final short MAX_FLAP_ANGLE_REFERENCE = 350;
	public int flapNotchSelected;
	public double flapDesiredAngle;
	public double flapCurrentAngle;
	
	//External state control.
	public boolean turningLeft;
	public boolean turningRight;
	public boolean autopilot;
	public byte turningCooldown;
	public double speedSetting;
	public double altitudeSetting;
	
	//Internal states.
	public boolean hasRotors;
	private double pitchDirectionFactor;
	private double currentWingArea;
	public double trackAngle;
	private final Set<EntityVehicleF_Physics> towedVehiclesCheckedForWeights = new HashSet<EntityVehicleF_Physics>();
	
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

	public EntityVehicleF_Physics(WrapperWorld world, WrapperNBT data){
		super(world, data);
		
		this.aileronAngle = (short) data.getInteger("aileronAngle");
		this.elevatorAngle = (short) data.getInteger("elevatorAngle");
		this.rudderAngle = (short) data.getInteger("rudderAngle");
		this.flapNotchSelected = data.getInteger("flapNotchSelected");
		this.flapCurrentAngle = data.getDouble("flapCurrentAngle");
		this.aileronTrim = (short) data.getInteger("aileronTrim");
		this.elevatorTrim = (short) data.getInteger("elevatorTrim");
		this.rudderTrim = (short) data.getInteger("rudderTrim");
		
		this.autopilot = data.getBoolean("autopilot");
		this.altitudeSetting = data.getDouble("altitudeSetting");
		this.speedSetting = data.getDouble("speedSetting");
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			if(definition.motorized.flapNotches != null && !definition.motorized.flapNotches.isEmpty()){
				flapDesiredAngle = definition.motorized.flapNotches.get(flapNotchSelected);
				//Adjust flaps to current setting.
				if(flapCurrentAngle < flapDesiredAngle){
					flapCurrentAngle += definition.motorized.flapSpeed;
				}else if(flapCurrentAngle > flapDesiredAngle){
					flapCurrentAngle -= definition.motorized.flapSpeed;
				}
				if(Math.abs(flapCurrentAngle - flapDesiredAngle) < definition.motorized.flapSpeed){
					flapCurrentAngle = flapDesiredAngle;
				}
			}
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
				if(connection.hookupBaseEntity instanceof EntityVehicleF_Physics){
					otherVehicle = (EntityVehicleF_Physics) connection.hookupBaseEntity;
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
			}
			//If we still have a vehicle reference, we didn't exit cleanly and need to disconnect it.
			if(otherVehicle != null){
				disconnectTrailer(otherVehicle.towedByConnection);
			}
		}
		return combinedMass;
	}
	
	@Override
	protected float getSteeringAngle(){
		return -rudderAngle/(float)MAX_RUDDER_ANGLE;
	}
	
	@Override
	protected void addToSteeringAngle(float degrees){
		short delta = (short) (-degrees*10);
		if(rudderAngle + delta > MAX_RUDDER_ANGLE){
			delta = (short) (MAX_RUDDER_ANGLE - rudderAngle);
		}else if(rudderAngle + delta < -MAX_RUDDER_ANGLE){
			delta = (short) (-MAX_RUDDER_ANGLE - rudderAngle);
		}
		rudderAngle += delta;
		rudderCooldown = 20;
		InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.RUDDER, delta, rudderCooldown));
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
				if(isRotor){
					hasRotors = true;
					rotorRotation.set((-elevatorAngle - angles.x*10)/MAX_ELEVATOR_ANGLE, -5D*rudderAngle/MAX_RUDDER_ANGLE, (aileronAngle - angles.z*10)/MAX_AILERON_ANGLE);
				}else{
					rotorRotation.set(0, 0, 0);
				}
			}
			
			//Get forces.  Some forces are specific to JSON sections.
			//First get gravity.
			gravitationalForce = definition.motorized.ballastVolume == 0 ? currentMass*(9.8/400) : 0;
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
					rudderCooldown = aileronCooldown;
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
			aileronLiftCoeff = getLiftCoeff((aileronAngle + aileronTrim)/10F, 2);
			elevatorLiftCoeff = getLiftCoeff(-2.5 + trackAngle - (elevatorAngle + elevatorTrim)/10F, 2);
			rudderLiftCoeff = getLiftCoeff((rudderAngle + rudderTrim)/10F - yawAngleDelta, 2);
			currentWingArea = definition.motorized.wingArea + definition.motorized.wingArea*0.15D*flapCurrentAngle/MAX_FLAP_ANGLE_REFERENCE;
			
			//Get the drag coefficient and force.
			if(definition.motorized.isBlimp){
				dragCoeff = 0.004F*Math.pow(Math.abs(yawAngleDelta), 2) + (definition.motorized.dragCoefficient != 0 ? definition.motorized.dragCoefficient : 0.03D);
			}else if(definition.motorized.isAircraft){
				//Aircraft are 0.03 by default, or whatever is specified.
				dragCoeff = 0.0004F*Math.pow(trackAngle, 2) + (definition.motorized.dragCoefficient != 0 ? definition.motorized.dragCoefficient : 0.03D);
			}else{
				dragCoeff = definition.motorized.dragCoefficient != 0 ? definition.motorized.dragCoefficient : 2.0D;
				//If we aren't an aircraft, check for grounded ground devices.
				//If we don't have any grounded ground devices, assume we are in the air or in water.
				//This results in an increase in drag due to poor airflow.
				if(groundDeviceCollective.groundedGroundDevices.isEmpty()){
					dragCoeff *= 3D;
				}
			}
			if(definition.motorized.crossSectionalArea > 0){
				dragForce = 0.5F*airDensity*velocity*velocity*definition.motorized.crossSectionalArea*dragCoeff;
			}else if(definition.motorized.wingSpan > 0){
				dragForce = 0.5F*airDensity*velocity*velocity*currentWingArea*(dragCoeff + wingLiftCoeff*wingLiftCoeff/(Math.PI*definition.motorized.wingSpan*definition.motorized.wingSpan/currentWingArea*0.8));
			}else{
				dragForce = 0.5F*airDensity*velocity*velocity*5.0F*dragCoeff;
			}
			
			//Get ballast force.
			if(definition.motorized.ballastVolume > 0){
				//Ballast gets less effective at applying positive lift at higher altitudes.
				//This prevents blimps from ascending into space.
				//Also take into account motionY, as we should provide less force if we are already going in the same direction.
				if(elevatorAngle < 0){
					ballastForce = airDensity*definition.motorized.ballastVolume*-elevatorAngle/100D;
				}else if(elevatorAngle > 0){
					ballastForce = 1.225*definition.motorized.ballastVolume*-elevatorAngle/100D;
				}else{
					ballastForce = 1.225*definition.motorized.ballastVolume*10D*-motion.y;
				}
				if(motion.y*ballastForce != 0){
					ballastForce /= Math.pow(1 + Math.abs(motion.y), 2);
				}
			}
			
			//Get all other forces.
			wingForce = 0.5F*airDensity*axialVelocity*axialVelocity*currentWingArea*wingLiftCoeff;
			aileronForce = 0.5F*airDensity*axialVelocity*axialVelocity*definition.motorized.aileronArea*aileronLiftCoeff;
			elevatorForce = 0.5F*airDensity*axialVelocity*axialVelocity*definition.motorized.elevatorArea*elevatorLiftCoeff;			
			rudderForce = 0.5F*airDensity*axialVelocity*axialVelocity*definition.motorized.rudderArea*rudderLiftCoeff;
			
			//Get torques.  Point for ailerons is 0.75% to the edge of the wing.
			aileronTorque = aileronForce*definition.motorized.wingSpan*0.5F*0.75F;
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
			if(definition.motorized.wingArea > 0 && trackAngle > 40 && angles.x < 45 && !groundDeviceCollective.isAnythingOnGround()){
				elevatorTorque += 100;
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
		}else{
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
		}
	}
	
	@Override
	protected void dampenControlSurfaces(){
		if(!definition.motorized.isAircraft && autopilot){
			if(velocity < speedSetting){
				if(throttle < MAX_THROTTLE){
					InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 1, (byte) 0));
					++throttle;
				}
			}else if(velocity > speedSetting){
				if(throttle > 0){
					InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.THROTTLE, (short) -1, (byte) 0));
					--throttle;
				}
			}
		}
		
		if(hasRotors){
			//Helicopter.  Do auto-hover code if required.
			if(autopilot){
				//Change throttle to maintain altitude.
				//Only do this once every 1/2 second to allow for thrust changes.
				if(world.getTick()%10 == 0){
					if(motion.y < 0 && throttle < MAX_THROTTLE){
						InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.THROTTLE, ++throttle, Byte.MAX_VALUE));
					}else if(motion.y > 0 && throttle < MAX_THROTTLE){
						InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.THROTTLE, --throttle, Byte.MAX_VALUE));
					}
				}
				//Change pitch/roll based on movement.
				double forwardsVelocity = motion.dotProduct(headingVector);
				double sidewaysVelocity = motion.dotProduct(sideVector);
				if(forwardsVelocity < 0 && elevatorTrim < MAX_ELEVATOR_TRIM){
					++elevatorTrim;
					InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, true));
				}else if(forwardsVelocity > 0 && elevatorTrim > -MAX_ELEVATOR_TRIM){
					--elevatorTrim;
					InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, false));
				}
				if(sidewaysVelocity < 0 && aileronTrim < MAX_AILERON_TRIM){
					++aileronTrim;
					InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, true));
				}else if(sidewaysVelocity > 0 && aileronTrim > -MAX_AILERON_TRIM){
					--aileronTrim;
					InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, false));
				}
			}else{
				//Reset trim to prevent directional surges.
				if(elevatorTrim < 0){
					++elevatorTrim;
					InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, true));
				}else if(elevatorTrim > 0){
					--elevatorTrim;
					InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, false));
				}
				if(aileronTrim < 0){
					++aileronTrim;
					InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, true));
				}else if(aileronTrim > 0){
					--aileronTrim;
					InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, false));
				}
			}
		}else if(definition.motorized.isAircraft && autopilot){
			//Normal aircraft.  Do autopilot operations if required.
			//If we are not flying at a steady elevation, angle the elevator to compensate
			if(-motion.y*100 > elevatorTrim + 1 && elevatorTrim < MAX_ELEVATOR_TRIM){
				++elevatorTrim;
				InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, true));
			}else if(-motion.y*100 < elevatorTrim - 1 && elevatorTrim > -MAX_ELEVATOR_TRIM){
				--elevatorTrim;
				InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, false));
			}
			//Keep the roll angle at 0.
			if(-angles.z > aileronTrim + 1 && aileronTrim < MAX_AILERON_TRIM){
				++aileronTrim;
				InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, true));
			}else if(-angles.z < aileronTrim - 1 && aileronTrim > -MAX_AILERON_TRIM){
				--aileronTrim;
				InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, false));
			}
		}
		
		if(aileronCooldown==0){
			if(aileronAngle != 0){
				if(aileronAngle < AILERON_DAMPEN_RATE && aileronAngle > -AILERON_DAMPEN_RATE){
					InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.AILERON, (short) -aileronAngle, (byte) 0));
					aileronAngle = 0;
				}else{
					InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.AILERON, aileronAngle < 0 ? AILERON_DAMPEN_RATE : -AILERON_DAMPEN_RATE, (byte) 0));
					aileronAngle += aileronAngle < 0 ? AILERON_DAMPEN_RATE : -AILERON_DAMPEN_RATE;
				}
			}
		}else{
			--aileronCooldown;
		}
		
		if(elevatorCooldown==0){
			if(elevatorAngle != 0){
				if(elevatorAngle < ELEVATOR_DAMPEN_RATE && elevatorAngle > -ELEVATOR_DAMPEN_RATE){
					InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.ELEVATOR, (short) -elevatorAngle, (byte) 0));
					elevatorAngle = 0;
				}else{
					InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.ELEVATOR, elevatorAngle < 0 ? ELEVATOR_DAMPEN_RATE : -ELEVATOR_DAMPEN_RATE, (byte) 0));
					elevatorAngle += elevatorAngle < 0 ? ELEVATOR_DAMPEN_RATE : -ELEVATOR_DAMPEN_RATE;
				}
			}
		}else{
			--elevatorCooldown;
		}
		
		if(rudderCooldown==0){
			if(rudderAngle != 0){
				if(rudderAngle < RUDDER_DAMPEN_RATE && rudderAngle > -RUDDER_DAMPEN_RATE){
					InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.RUDDER, (short) -rudderAngle, (byte) 0));
					rudderAngle = 0;
				}else{
					InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.RUDDER, rudderAngle < 0 ? RUDDER_DAMPEN_RATE : -RUDDER_DAMPEN_RATE, (byte) 0));
					rudderAngle += rudderAngle < 0 ? RUDDER_DAMPEN_RATE : -RUDDER_DAMPEN_RATE;
				}
			}
		}else{
			--rudderCooldown;
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
			return towedByConnection.hitchBaseEntity.getRawVariableValue(variable, partialTicks);
		}
				
		//If we have a variable with a suffix, we need to get that part first and pass
		//it into this method rather than trying to run through the code now.
		int partNumber = getVariableNumber(variable);
		if(partNumber != -1){
			return getSpecificPartAnimation(this, variable, partNumber, partialTicks);
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
			case("acceleration"): return motion.length() - prevMotion.length();

			//Vehicle state cases.
			case("throttle"): return throttle/(double)EntityVehicleF_Physics.MAX_THROTTLE;
			case("brake"): return brake/(double)EntityVehicleF_Physics.MAX_BRAKE;
			case("fuel"): return fuelTank.getFluidLevel()/fuelTank.getMaxLevel();
			case("electric_power"): return electricPower;
			case("electric_usage"): return electricFlow*20D;
			case("engines_on"): return enginesOn ? 1 : 0;
			case("engines_running"): return enginesRunning ? 1 : 0;
			case("p_brake"): return parkingBrakeOn ? 1 : 0;
			case("reverser"): return reverseThrust ? 1 : 0;
			case("horn"): return hornOn ? 1 : 0;
			case("autopilot"): return autopilot ? 1 : 0;
			case("locked"): return locked ? 1 : 0;
			case("door"): return parkingBrakeOn && velocity < 0.25 ? 1 : 0;
			case("fueling"): return beingFueled ? 1 : 0;
			
			//State cases generally used on aircraft.
			case("aileron"): return aileronAngle/10D;
			case("elevator"): return elevatorAngle/10D;
			case("rudder"): return rudderAngle/10D;
			case("flaps_setpoint"): return flapDesiredAngle;
			case("flaps_actual"): return flapCurrentAngle;
			case("flaps_moving"): return flapCurrentAngle != flapDesiredAngle ? 1 : 0;
			case("trim_aileron"): return aileronTrim/10D;
			case("trim_elevator"): return elevatorTrim/10D;
			case("trim_rudder"): return rudderTrim/10D;
			case("vertical_speed"): return motion.y*EntityVehicleF_Physics.SPEED_FACTOR*20;
			case("lift_reserve"): return -trackAngle;
			case("turn_coordinator"): return ((angles.z - prevAngles.z)/10 + angles.y - prevAngles.y)/0.15D*25;
			case("turn_indicator"): return (angles.y - prevAngles.y)/0.15F*25F;
			case("slip"): return 75*sideVector.dotProduct(normalizedVelocityVector);
			case("gear_setpoint"): return gearUpCommand ? 1 : 0;
			case("gear_moving"): return (gearUpCommand ? gearMovementTime != definition.motorized.gearSequenceDuration : gearMovementTime != 0) ? 1 : 0;
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
		data.setInteger("aileronAngle", aileronAngle);
		data.setInteger("elevatorAngle", elevatorAngle);
		data.setInteger("rudderAngle", rudderAngle);
		data.setInteger("flapNotchSelected", flapNotchSelected);
		data.setDouble("flapCurrentAngle", flapCurrentAngle);
		data.setInteger("aileronTrim", aileronTrim);
		data.setInteger("elevatorTrim", elevatorTrim);
		data.setInteger("rudderTrim", rudderTrim);

		data.setBoolean("autopilot", autopilot);
		data.setDouble("altitudeSetting", altitudeSetting);
		data.setDouble("speedSetting", speedSetting);
		return data;
	}
}
