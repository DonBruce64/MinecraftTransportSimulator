package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.rendering.instances.RenderVehicle;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;

/**This class adds the final layer of physics calculations on top of the
 * existing entity calculations.  Various control surfaces are present, as
 * well as helper functions and logic for controlling those surfaces.
 * 
 * @author don_bruce
 */
public class EntityVehicleF_Physics extends EntityVehicleE_Powered{
	//Note that angle variable should be divided by 10 to get actual angle.
	
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
	public static final short MAX_FLAP_ANGLE = 350;
	public short flapDesiredAngle;
	public short flapCurrentAngle;
	
	//External state control.
	public boolean autopilot;
	public boolean cruiseControl;
	public boolean turningLeft;
	public boolean turningRight;
	public byte turningCooldown;
	public double cruiseControlSpeed;
	public double altitudeSetting;
	
	//Internal states.
	private boolean updateThisCycle;
	public boolean isVTOL;
	private double pitchDirectionFactor;
	private double currentWingArea;
	public double trackAngle;
	private final List<EntityVehicleF_Physics> towedVehiclesCheckedForWeights = new ArrayList<EntityVehicleF_Physics>();
	
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
	private Point3d thrustForce = new Point3d(0D, 0D, 0D);//kg*m/ticks^2
	private Point3d totalAxialForce = new Point3d(0D, 0D, 0D);//kg*m/ticks^2
	private Point3d totalMotiveForce = new Point3d(0D, 0D, 0D);//kg*m/ticks^2
	private Point3d totalGlobalForce = new Point3d(0D, 0D, 0D);//kg*m/ticks^2
	private Point3d totalForce = new Point3d(0D, 0D, 0D);//kg*m/ticks^2
	
	//Torques.
	private double momentRoll;//kg*m^2
	private double momentPitch;//kg*m^2
	private double momentYaw;//kg*m^2
	private double aileronTorque;//kg*m^2/ticks^2
	private double elevatorTorque;//kg*m^2/ticks^2
	private double rudderTorque;//kg*m^2/ticks^2
	private Point3d thrustTorque = new Point3d(0D, 0D, 0D);//kg*m^2/ticks^2
	private Point3d totalTorque = new Point3d(0D, 0D, 0D);//kg*m^2/ticks^2
	private Point3d rotorRotation = new Point3d(0D, 0D, 0D);//degrees

	public EntityVehicleF_Physics(IWrapperWorld world, IWrapperNBT data){
		super(world, data);
		
		this.aileronAngle = (short) data.getInteger("aileronAngle");
		this.elevatorAngle = (short) data.getInteger("elevatorAngle");
		this.rudderAngle = (short) data.getInteger("rudderAngle");
		this.flapDesiredAngle = (short) data.getInteger("flapDesiredAngle");
		this.flapCurrentAngle = (short) data.getInteger("flapCurrentAngle");
		this.aileronTrim = (short) data.getInteger("aileronTrim");
		this.elevatorTrim = (short) data.getInteger("elevatorTrim");
		this.rudderTrim = (short) data.getInteger("rudderTrim");
	}
	
	@Override
	public void update(){
		//If we are a towed trailer, and we aren't scheduled for an update, skip this cycle.
		//Instead, we get called to update from the vehicle we are being towed by.
		//If we are updating from that vehicle, we'll have the flag set to not return here.
		if(towedByVehicle != null && !updateThisCycle){
			return;
		}else{
			updateThisCycle = false;
		}
		
		//Do movement and all other updates.
		super.update();
		
		//Turn on brake lights and indicator lights.
		if(brakeOn){
			lightsOn.add(LightType.BRAKELIGHT);
			if(lightsOn.contains(LightType.LEFTTURNLIGHT)){
				lightsOn.remove(LightType.LEFTINDICATORLIGHT);
			}else{
				lightsOn.add(LightType.LEFTINDICATORLIGHT);
			}
			if(lightsOn.contains(LightType.RIGHTTURNLIGHT)){
				lightsOn.remove(LightType.RIGHTINDICATORLIGHT);
			}else{
				lightsOn.add(LightType.RIGHTINDICATORLIGHT);
			}
		}else{
			lightsOn.remove(LightType.BRAKELIGHT);
			lightsOn.remove(LightType.LEFTINDICATORLIGHT);
			lightsOn.remove(LightType.RIGHTINDICATORLIGHT);
		}
		
		//Set backup light state.
		lightsOn.remove(LightType.BACKUPLIGHT);
		for(PartEngine engine : engines.values()){
			if(engine.currentGear < 0){
				lightsOn.add(LightType.BACKUPLIGHT);
				break;
			}
		}
		
		//Adjust flaps to current setting.
		if(flapCurrentAngle < flapDesiredAngle){
			++flapCurrentAngle;
		}else if(flapCurrentAngle > flapDesiredAngle){
			--flapCurrentAngle;
		}
		
		//If we are towing a vehicle, update it now.
		if(towedVehicle != null){
			towedVehicle.updateThisCycle = true;
			towedVehicle.update();
		}
	}
	
	@Override
	protected float getCurrentMass(){
		//Need to use a list here to make sure we don't end up with infinite recursion due to bad trailer linkings.
		//This could lock up a world if not detected!
		if(towedVehicle != null){
			if(towedVehiclesCheckedForWeights.contains(this)){
				MasterLoader.coreInterface.logError("ERROR: Infinite loop detected on weight checking code!  Is a trailer towing the thing that's towing it?");
				towedVehicle.towedByVehicle = null;
				towedVehicle = null;
				return super.getCurrentMass();
			}else{
				towedVehiclesCheckedForWeights.add(this);
				float combinedMass = super.getCurrentMass() + towedVehicle.getCurrentMass();
				towedVehiclesCheckedForWeights.clear();
				return combinedMass;
			}
		}else{
			return super.getCurrentMass();
		}
	}
	
	@Override
	protected float getSteeringAngle(){
		return -rudderAngle/10F;
	}
	
	@Override
	protected void getForcesAndMotions(){
		//If we are free, do normal updates.  But if we are towed by a vehicle, do trailer forces instead.
		//This prevents trailers from behaving badly and flinging themselves into the abyss.
		if(towedByVehicle == null){
			//Set moments.
			momentRoll = definition.general.emptyMass*(1.5F + fuelTank.getFluidLevel()/10000F);
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
				
				//If the part is a propeller or jet engine, we add thrust torque.
				//If it's a rotor, we also add control surface torque.
				//Torque added is relative to the propeller force output, factored by the angle of the control surface.
				if(isPropeller || jetPower > 0){
					thrustTorque.add(partForce.y*-part.placementOffset.z, partForce.z*part.placementOffset.x, partForce.y*part.placementOffset.x);
				}
				if(isRotor){
					isVTOL = true;
					if(!autopilot){
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
			}
			
			//Get forces.  Some forces are specific to JSON sections.
			//First get gravity.
			gravitationalForce = definition.motorized.ballastVolume == 0 ? currentMass*(9.8/400) : 0;
			
			//Get the track angle.  This is used for control surfaces.
			trackAngle = -Math.toDegrees(Math.asin(verticalVector.dotProduct(normalizedVelocityVector)));
			
			//Get the lift coefficients and states for control surfaces.
			wingLiftCoeff = getLiftCoeff(trackAngle, 2 + flapCurrentAngle/(double)MAX_FLAP_ANGLE);
			aileronLiftCoeff = getLiftCoeff((aileronAngle + aileronTrim)/10F, 2);
			elevatorLiftCoeff = getLiftCoeff(-2.5 + trackAngle - (elevatorAngle + elevatorTrim)/10F, 2);
			rudderLiftCoeff = getLiftCoeff((rudderAngle + rudderTrim)/10F - Math.toDegrees(Math.asin(sideVector.dotProduct(normalizedVelocityVector))), 2);
			currentWingArea = definition.motorized.wingArea + definition.motorized.wingArea*0.15D*flapCurrentAngle/MAX_FLAP_ANGLE;
			
			//Set blimp-specific states before calculating forces.
			if(definition.general.isBlimp){
				//Blimps are turned with rudders, not ailerons.  This puts the keys at an odd location.  To compensate, 
				//we set the rudder to the aileron if the aileron is greater or less than the rudder.  That way no matter 
				//which key is pressed, they both activate the rudder for turning.
				if((aileronAngle < 0 && aileronAngle < rudderAngle) || (aileronAngle > 0 && aileronAngle > rudderAngle)){
					rudderAngle = aileronAngle;
					rudderCooldown = aileronCooldown;
				}
				
				//If the throttle is idle, and we have the brake pressed at a slow speed, stop the blimp.
				//This is needed to prevent runaway blimps.
				if(throttle == 0 && Math.abs(velocity) < 0.15 && (brakeOn || parkingBrakeOn)){
					motion.x = 0;
					motion.z = 0;
					thrustForce.set(0D, 0D, 0D);
					thrustTorque.set(0D, 0D, 0D);
				}
			}
			
			//Get the drag coefficient and force.
			if(definition.general.isAircraft){
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
			
			//Do more blimp-specific things for the forces.
			if(definition.general.isBlimp){
				//Roll and pitch are applied only if we aren't level.
				//This only happens if we fall out of the sky and land on the ground and tilt.
				if(angles.z > 0){
					aileronTorque = -Math.min(0.5F, angles.z)*currentMass;
				}else if(angles.z < 0){
					aileronTorque = -Math.max(-0.5F, angles.z)*currentMass;
				}else{
					aileronTorque = 0;
				}
				if(angles.x > 0){
					elevatorTorque = -Math.min(0.5F, angles.x)*currentMass;
				}else if(angles.x < 0){
					elevatorTorque = -Math.max(-0.5F, angles.x)*currentMass;
				}else{
					elevatorTorque = 0;
				}
			}
			
			//As a special case, if the vehicle is a stalled plane, add a forwards pitch to allow the plane to right itself.
			//This is needed to prevent the plane from getting stuck in a vertical position and crashing.
			if(definition.motorized.wingArea > 0 && trackAngle > 40 && angles.x < 45 && groundDeviceCollective.groundedGroundDevices.isEmpty()){
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
			//Need to apply both motion to move the trailer, and yaw to adjust the trailer's angle relative to the truck.
			//Yaw is applied based on the current and next position of the truck's hookup.
			//Motion is applied after yaw corrections to ensure the trailer follows the truck.
			//Start by getting the hitch offsets.  We save the current offset as we'll change it for angle calculations.
			Point3d tractorHitchPrevOffset = towedByVehicle.definition.motorized.hitchPos.copy().rotateFine(towedByVehicle.prevAngles).add(towedByVehicle.prevPosition).subtract(prevPosition);
			Point3d tractorHitchCurrentOffset = towedByVehicle.definition.motorized.hitchPos.copy().rotateFine(towedByVehicle.angles).add(towedByVehicle.position).subtract(position);
			Point3d tractorHitchOffset = tractorHitchCurrentOffset.copy();
			
			//Now calculate how much yaw we need to apply to rotate the trailer.
			//This is only done for the X and Z motions.
			tractorHitchPrevOffset.y = 0;
			tractorHitchCurrentOffset.y = 0;
			tractorHitchPrevOffset.normalize();
			tractorHitchCurrentOffset.normalize();
			double rotationDelta = Math.toDegrees(Math.acos(tractorHitchPrevOffset.dotProduct(tractorHitchCurrentOffset)));
			rotationDelta *= Math.signum(tractorHitchPrevOffset.crossProduct(tractorHitchCurrentOffset).y);
			
			//If the rotation is valid, add it.
			//We need to fake-add the yaw for the motion calculation here, hence the odd temp setting of the angles.
			Point3d trailerHookupOffset;
			if(!Double.isNaN(rotationDelta)){
				rotation.y = rotationDelta;
				angles.y += rotationDelta;
				trailerHookupOffset = definition.motorized.hookupPos.copy().rotateFine(angles);
				angles.y -= rotationDelta;
			}else{
				trailerHookupOffset = definition.motorized.hookupPos.copy().rotateFine(angles);
			}
			
			//Now move the trailer to the hitch.  Also set rotations to 0 to prevent odd math.
			motion.setTo(tractorHitchOffset.subtract(trailerHookupOffset).multiply(1/SPEED_FACTOR));
			rotation.x = 0;
			rotation.z = 0;
		}
	}
	
	@Override
	protected void dampenControlSurfaces(){
		if(cruiseControl){
			if(velocity < cruiseControlSpeed){
				if(throttle < 100){
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 1, (byte) 0));
					++throttle;
				}
			}else if(velocity > cruiseControlSpeed){
				if(throttle > 0){
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.THROTTLE, (short) -1, (byte) 0));
					--throttle;
				}
			}
		}
		
		if(isVTOL){
			//VTOL craft.  Do auto-hover code if required.
			if(autopilot){
				//Change throttle to maintain altitude.
				//Only do this once every 1/2 second to allow for thrust changes.
				if(world.getTime()%10 == 0){
					if(motion.y < 0 && throttle < 100){
						MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.THROTTLE, ++throttle, (byte) 0));
					}else if(motion.y > 0 && throttle < 100){
						MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.THROTTLE, --throttle, (byte) 0));
					}
				}
				//Change pitch/roll based on movement.
				double forwardsVelocity = motion.dotProduct(headingVector);
				double sidewaysVelocity = motion.dotProduct(sideVector);
				if(forwardsVelocity < 0 && elevatorTrim < MAX_ELEVATOR_TRIM){
					++elevatorTrim;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, true));
				}else if(forwardsVelocity > 0 && elevatorTrim > -MAX_ELEVATOR_TRIM){
					--elevatorTrim;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, false));
				}
				if(sidewaysVelocity < 0 && aileronTrim < MAX_AILERON_TRIM){
					++aileronTrim;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, true));
				}else if(sidewaysVelocity > 0 && aileronTrim > -MAX_AILERON_TRIM){
					--aileronTrim;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, false));
				}
			}else{
				//Reset trim to prevent directional surges.
				if(elevatorTrim < 0){
					++elevatorTrim;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, true));
				}else if(elevatorTrim > 0){
					--elevatorTrim;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, false));
				}
				if(aileronTrim < 0){
					++aileronTrim;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, true));
				}else if(aileronTrim > 0){
					--aileronTrim;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, false));
				}
			}
		}else{
			//Normal aircraft.  Do autopilot operations if required.
			if(autopilot){
				//If we are not flying at a steady elevation, angle the elevator to compensate
				if(-motion.y*100 > elevatorTrim + 1 && elevatorTrim < MAX_ELEVATOR_TRIM){
					++elevatorTrim;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, true));
				}else if(-motion.y*100 < elevatorTrim - 1 && elevatorTrim > -MAX_ELEVATOR_TRIM){
					--elevatorTrim;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, false));
				}
				//Keep the roll angle at 0.
				if(-angles.z > aileronTrim + 1 && aileronTrim < MAX_AILERON_TRIM){
					++aileronTrim;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, true));
				}else if(-angles.z < aileronTrim - 1 && aileronTrim > -MAX_AILERON_TRIM){
					--aileronTrim;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, false));
				}
			}
		}
		
		if(aileronCooldown==0){
			if(aileronAngle != 0){
				if(aileronAngle < AILERON_DAMPEN_RATE && aileronAngle > -AILERON_DAMPEN_RATE){
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.AILERON, (short) -aileronAngle, (byte) 0));
					aileronAngle = 0;
				}else{
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.AILERON, aileronAngle < 0 ? AILERON_DAMPEN_RATE : -AILERON_DAMPEN_RATE, (byte) 0));
					aileronAngle += aileronAngle < 0 ? AILERON_DAMPEN_RATE : -AILERON_DAMPEN_RATE;
				}
			}
		}else{
			--aileronCooldown;
		}
		
		if(elevatorCooldown==0){
			if(elevatorAngle != 0){
				if(elevatorAngle < ELEVATOR_DAMPEN_RATE && elevatorAngle > -ELEVATOR_DAMPEN_RATE){
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.ELEVATOR, (short) -elevatorAngle, (byte) 0));
					elevatorAngle = 0;
				}else{
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.ELEVATOR, elevatorAngle < 0 ? ELEVATOR_DAMPEN_RATE : -ELEVATOR_DAMPEN_RATE, (byte) 0));
					elevatorAngle += elevatorAngle < 0 ? ELEVATOR_DAMPEN_RATE : -ELEVATOR_DAMPEN_RATE;
				}
			}
		}else{
			--elevatorCooldown;
		}
		
		if(rudderCooldown==0){
			if(rudderAngle != 0){
				if(rudderAngle < RUDDER_DAMPEN_RATE && rudderAngle > -RUDDER_DAMPEN_RATE){
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.RUDDER, (short) -rudderAngle, (byte) 0));
					rudderAngle = 0;
				}else{
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.RUDDER, rudderAngle < 0 ? RUDDER_DAMPEN_RATE : -RUDDER_DAMPEN_RATE, (byte) 0));
					rudderAngle += rudderAngle < 0 ? RUDDER_DAMPEN_RATE : -RUDDER_DAMPEN_RATE;
				}
			}
		}else{
			--rudderCooldown;
		}
	}
	
	@Override
	public void render(float partialTicks){
		RenderVehicle.render(this, partialTicks);
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
	public void save(IWrapperNBT data){
		super.save(data);
		data.setInteger("aileronAngle", aileronAngle);
		data.setInteger("elevatorAngle", elevatorAngle);
		data.setInteger("rudderAngle", rudderAngle);
		data.setInteger("flapDesiredAngle", flapDesiredAngle);
		data.setInteger("flapCurrentAngle", flapCurrentAngle);
		data.setInteger("aileronTrim", aileronTrim);
		data.setInteger("elevatorTrim", elevatorTrim);
		data.setInteger("rudderTrim", rudderTrim);
	}
}
