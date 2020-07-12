package minecrafttransportsimulator.vehicles.main;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;
import minecrafttransportsimulator.wrappers.WrapperNetwork;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

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
	public static final short AILERON_DAMPEN_RATE = 6;
	public short aileronAngle;
	public short aileronTrim;
	public byte aileronCooldown;
	
	//Elevator.
	public static final short MAX_ELEVATOR_ANGLE = 250;
	public static final short ELEVATOR_DAMPEN_RATE = 6;
	public short elevatorAngle;
	public short elevatorTrim;
	public byte elevatorCooldown;
	
	//Rudder.
	public static final short MAX_RUDDER_ANGLE = 450;
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
	public double cruiseControlSpeed;
	
	//Internal states.
	private boolean updateThisCycle;
	private boolean turningLeft;
	private boolean turningRight;
	private byte turningCooldown;
	public byte towingAngle;
	//private double towingDeltaYaw;
	private double pitchDirectionFactor;
	public double trackAngle;
	private double currentWingArea;
	//private Point3d hookupOffset;
	//private Point3d hookupPos;
	//private Point3d hitchOffset;
	//private Point3d hitchOffset2;
	//private Point3d hitchPos;
	//private Point3d hitchPos2;
	//private Point3d xzPlaneDelta;
	//private Point3d xzPlaneHeading;
	
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
	private float momentRoll;//kg*m^2
	private float momentPitch;//kg*m^2
	private float momentYaw;//kg*m^2
	private double aileronTorque;//kg*m^2/ticks^2
	private double elevatorTorque;//kg*m^2/ticks^2
	private double rudderTorque;//kg*m^2/ticks^2
	private Point3d thrustTorque = new Point3d(0D, 0D, 0D);//kg*m^2/ticks^2
	private Point3d totalTorque = new Point3d(0D, 0D, 0D);//kg*m^2/ticks^2

			
	public EntityVehicleF_Physics(World world){
		super(world);
	}
	
	public EntityVehicleF_Physics(World world, float posX, float posY, float posZ, float rotation, JSONVehicle definition){
		super(world, posX, posY, posZ, rotation, definition);
	}
	
	@Override
	public void onEntityUpdate(){
		//If we are a towed trailer, and we aren't scheduled for an update, skip this cycle.
		//Instead, we get called to update from the vehicle we are being towed by.
		//If we are updating from that vehicle, we'll have the flag set to not return here.
		if(towedByVehicle != null && !updateThisCycle){
			//TODO make this un-commented when trailers have proper physics.
			//return;
		}else{
			updateThisCycle = false;
		}
		
		super.onEntityUpdate();
		if(definition != null){
			//Change turn signal status depending on turning status.
			//Keep signals on until we have been moving without turning in the
			//pressed direction for 2 seconds, or if we turn in the other direction.
			if(rudderAngle < -200){
				turningLeft = true;
				turningCooldown = 40;
				lightsOn.add(LightType.LEFTTURNLIGHT);
			}else if(rudderAngle > 200){
				turningRight = true;
				turningCooldown = 40;
				lightsOn.add(LightType.RIGHTTURNLIGHT);
			}
			if(turningLeft && (rudderAngle > 0 || turningCooldown == 0)){
				turningLeft = false;
				lightsOn.remove(LightType.LEFTTURNLIGHT);
			}
			if(turningRight && (rudderAngle < 0 || turningCooldown == 0)){
				turningRight = false;
				lightsOn.remove(LightType.RIGHTTURNLIGHT);
			}
			if(velocity != 0 && turningCooldown > 0 && rudderAngle == 0){
				--turningCooldown;
			}
			
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
		}
		
		//Update towing angle.
		if(towedByVehicle != null){
			if(towingAngle < 30){
				++towingAngle;
			}
		}else{
			if(towingAngle > 0){
				--towingAngle;
			}
		}
		
		//If we are towing a vehicle, update it now.
		if(towedVehicle != null){
			towedVehicle.updateThisCycle = true;
			towedVehicle.onEntityUpdate();
		}
	}
	
	@Override
	protected float getCurrentMass(){
		if(towedVehicle != null){
			return super.getCurrentMass() + towedVehicle.getCurrentMass();
		}else{
			return super.getCurrentMass();
		}
	}
	
	@Override
	protected void getForcesAndMotions(){
		//If we are free, do normal updates.  But if we are towed by a vehicle, do trailer forces instead.
		//This prevents trailers from behaving badly and flinging themselves into the abyss.
		if(towedByVehicle == null){
			//Set moments.
			momentRoll = (float) (definition.general.emptyMass*(1.5F+(fuel/10000F)));
			momentPitch = (float) (2*currentMass);
			momentYaw = (float) (3*currentMass);
			
			//Get engine thrust force contributions.
			thrustForce.set(0D, 0D, 0D);
			thrustTorque.set(0D, 0D, 0D);
			for(PartEngine engine : engines.values()){
				Point3d engineForce = engine.getForceOutput();
				thrustForce.add(engineForce);
				thrustTorque.add(engineForce.y*-engine.placementOffset.z, engineForce.z*engine.placementOffset.x, engineForce.y*engine.placementOffset.x);
				//If the engine has a rotor add engine torque.
				//Torque added is relative to the engine force output, factored by the angle of the control surface.
				for(APart part : engine.childParts){
					if(part instanceof PartPropeller){
						if(part.definition.propeller.isRotor){
							double engineTotalForce = 5D*engineForce.length();
							double rollTorque = aileronAngle != 0 ? engineTotalForce*aileronAngle/MAX_AILERON_ANGLE - rotationRoll : (rotationRoll > 0 ? -Math.min(rotationRoll, 5) : Math.min(-rotationRoll, 5));
							double pitchTorque = elevatorAngle != 0 ? engineTotalForce*-elevatorAngle/MAX_ELEVATOR_ANGLE - rotationPitch : (rotationPitch > 0 ? -Math.min(rotationPitch, 5) : Math.min(-rotationPitch, 5));
							double yawTorque =  engineTotalForce*rudderAngle/MAX_RUDDER_ANGLE;
							thrustTorque.add(pitchTorque, yawTorque, rollTorque);
						}
					}
				}
			}
			
			//Get forces.  Some forces are specific to JSON sections.
			gravitationalForce = currentMass*(9.8/400);
			if(definition.blimp != null || definition.plane != null){
				//We are an aircraft.  We need to set our trackAngle here for forces, as well as the drag coefficient.
				trackAngle = -Math.toDegrees(Math.asin(verticalVector.dotProduct(velocityVector)));
				dragCoeff = 0.0004F*Math.pow(trackAngle, 2) + 0.03F;
			}else{
				//Not an aircraft.  We're either a car, or something else entirely.
				//If we don't have any grounded ground devices, assume we are in the air or in water.
				//In both cases, we need to increase drag.  If we aren't a car, just set a constant 2.0 coefficient.
				dragCoeff = definition.car != null ? (groundedGroundDevices.isEmpty() ? definition.car.dragCoefficient*3F : definition.car.dragCoefficient) : 2.0F; 
				dragForce = 0.5F*airDensity*velocity*velocity*5.0F*dragCoeff;
			}
			
			if(definition.blimp != null){
				//Blimps are turned with rudders, not ailerons.  This puts the keys at an odd location.  To compensate, 
				//we set the rudder to the aileron if the aileron is greater or less than the rudder.  That way no matter 
				//which key is pressed, they both activate the rudder for turning.
				//Blimps also have more drag than planes, so we 
				if((aileronAngle < 0 && aileronAngle < rudderAngle) || (aileronAngle > 0 && aileronAngle > rudderAngle)){
					rudderAngle = aileronAngle;
					rudderCooldown = aileronCooldown;
				}
				
				//Set coefficients.
				elevatorLiftCoeff = getLiftCoeff(-2.5 + trackAngle - (elevatorAngle + elevatorTrim)/10F, 2);
				rudderLiftCoeff = getLiftCoeff((rudderAngle + rudderTrim)/10F + Math.toDegrees(Math.asin(sideVector.dotProduct(velocityVector))), 2);
				
				//Get forces.  Set gravity to 0 as blimps aren't affected by that.
				dragForce = 0.5F*airDensity*velocity*velocity*definition.blimp.crossSectionalArea*dragCoeff;		
				rudderForce = 0.5F*airDensity*velocity*velocity*definition.blimp.rudderArea*rudderLiftCoeff;
				gravitationalForce = 0;
				
				//Get torques.
				rudderTorque = rudderForce*definition.blimp.tailDistance;
				
				//Ballast gets less effective at applying positive lift at higher altitudes.
				//This prevents blimps from ascending into space.
				//Also take into account motionY, as we should provide less force if we are already going in the same direction.
				if(elevatorAngle < 0){
					ballastForce = airDensity*definition.blimp.ballastVolume*-elevatorAngle/100D;
				}else if(elevatorAngle > 0){
					ballastForce = 1.225*definition.blimp.ballastVolume*-elevatorAngle/100D;
				}else{
					ballastForce = 1.225*definition.blimp.ballastVolume*10D*-motionY;
				}
				if(motionY*ballastForce != 0){
					ballastForce /= Math.pow(1 + Math.abs(motionY), 2);
				}
				
				//If the throttle is idle, and we have the brake pressed at a slow speed, stop the blimp.
				//This is needed to prevent runaway blimps.
				if(throttle == 0 && Math.abs(velocity) < 0.15 && (brakeOn || parkingBrakeOn)){
					motionX = 0;
					motionZ = 0;
					thrustForce.set(0D, 0D, 0D);
					thrustTorque.set(0D, 0D, 0D);
				}
				
				//Roll and pitch are applied only if we aren't level.
				//This only happens if we fall out of the sky and land on the ground and tilt.
				if(rotationRoll > 0){
					aileronTorque = -Math.min(0.5F, rotationRoll)*currentMass;
				}else if(rotationRoll < 0){
					aileronTorque = -Math.max(-0.5F, rotationRoll)*currentMass;
				}else{
					aileronTorque = 0;
				}
				if(rotationPitch > 0){
					elevatorTorque = -Math.min(0.5F, rotationPitch)*currentMass;
				}else if(rotationPitch < 0){
					elevatorTorque = -Math.max(-0.5F, rotationPitch)*currentMass;
				}else{
					elevatorTorque = 0;
				}
			}else if(definition.plane != null){
				//Adjust flaps to current setting.  If no flaps are present, then this will never change.
				if(flapCurrentAngle < flapDesiredAngle){
					++flapCurrentAngle;
				}else if(flapCurrentAngle > flapDesiredAngle){
					--flapCurrentAngle;
				}
				
				//Set coefficients and areas.
				wingLiftCoeff = getLiftCoeff(trackAngle, 2 + flapCurrentAngle/(double)MAX_FLAP_ANGLE);
				aileronLiftCoeff = getLiftCoeff((aileronAngle + aileronTrim)/10F, 2);
				elevatorLiftCoeff = getLiftCoeff(-2.5 + trackAngle - (elevatorAngle + elevatorTrim)/10F, 2);
				rudderLiftCoeff = getLiftCoeff((rudderAngle + rudderTrim)/10F + Math.toDegrees(Math.asin(sideVector.dotProduct(velocityVector))), 2);
				currentWingArea = definition.plane.wingArea + definition.plane.wingArea*0.15D*flapCurrentAngle/MAX_FLAP_ANGLE;
				
				//Get forces.
				dragForce = 0.5F*airDensity*velocity*velocity*currentWingArea*(dragCoeff + wingLiftCoeff*wingLiftCoeff/(Math.PI*definition.plane.wingSpan*definition.plane.wingSpan/currentWingArea*0.8));
				wingForce = 0.5F*airDensity*velocity*velocity*currentWingArea*wingLiftCoeff;
				aileronForce = 0.5F*airDensity*velocity*velocity*definition.plane.aileronArea*aileronLiftCoeff;
				elevatorForce = 0.5F*airDensity*velocity*velocity*definition.plane.elevatorArea*elevatorLiftCoeff;			
				rudderForce = 0.5F*airDensity*velocity*velocity*definition.plane.rudderArea*rudderLiftCoeff;
				
				//Get torques.
				aileronTorque = aileronForce*definition.plane.wingSpan*0.5F*0.75F;
				elevatorTorque = elevatorForce*definition.plane.tailDistance;
				rudderTorque = rudderForce*definition.plane.tailDistance;
				
				//As a special case, if the plane is pointed upwards and stalling, add a forwards pitch to allow the plane to right itself.
				//This is needed to prevent the plane from getting stuck in a vertical position and crashing.
				if(velocity < 0.25 && groundedGroundDevices.isEmpty()){
					if(rotationPitch < -45){
						elevatorTorque += 100;
					}
				}
			}
			
			//Add all forces to the main force matrix and apply them.
			totalAxialForce.set(0D, wingForce + elevatorForce, 0D).add(thrustForce);
			totalAxialForce = RotationSystem.getRotatedPoint(totalAxialForce, rotationPitch, rotationYaw, rotationRoll);
			totalMotiveForce.set(-dragForce, -dragForce, -dragForce).multiply(velocityVector);
			totalGlobalForce.set(0D, ballastForce - gravitationalForce, 0D);
			totalForce.set(0D, 0D, 0D).add(totalAxialForce).add(totalMotiveForce).add(totalGlobalForce).multiply(1/currentMass);
			
			motionX += totalForce.x;
			motionY += totalForce.y;
			motionZ += totalForce.z;
			
			//Add all torques to the main torque matrix and apply them.
			pitchDirectionFactor = Math.abs(rotationRoll%360);
			pitchDirectionFactor = pitchDirectionFactor < 90 || pitchDirectionFactor > 270 ? 1.0D : -1.0D;
			totalTorque.set(elevatorTorque, rudderTorque, aileronTorque).add(thrustTorque).multiply(180D/Math.PI);
			motionRoll = (float) totalTorque.z/momentRoll;
			motionPitch = (float) (pitchDirectionFactor*(1-Math.abs(sideVector.y))*totalTorque.x - sideVector.y*totalTorque.y)/momentPitch;
			motionYaw = (float) (sideVector.y*totalTorque.x - verticalVector.y*-totalTorque.y)/momentYaw;
			
			if(world.isRemote && ConfigSystem.configObject.client.devMode.value){
				System.out.format("States: Mass:%f TrackAngle:%f WingArea:%f\n", currentMass, trackAngle, currentWingArea);
				System.out.format("Forces: Engine:%f Drag:%f Wing/Elevator:%f Ballast:%f Gravity:%f\n", thrustForce.length(), dragForce, wingForce + elevatorForce, ballastForce, gravitationalForce);
				System.out.format("Torques: Engine:%f Elevator:%f Rudder:%f Aileron:%f\n", thrustTorque.length(), elevatorTorque, rudderTorque, aileronTorque);
			}
		}else{
			///START OF NEW CODE.
			/*
			//Get the vector from this trailer's center position to the hitch of the towing vehicle.
			Point3d tractorHitchOffset = new Point3d(towedByVehicle.definition.motorized.hitchPos[0], towedByVehicle.definition.motorized.hitchPos[1], towedByVehicle.definition.motorized.hitchPos[2]);
			tractorHitchOffset = RotationSystem.getRotatedPoint(tractorHitchOffset, rotationPitch, rotationYaw, rotationRoll).add(towedByVehicle.positionVector).subtract(positionVector);
			
			//Get the vector for the current hookup position on this vehicle.
			Point3d trailerHookupOffset = new Point3d(definition.motorized.hookupPos[0], definition.motorized.hookupPos[1], definition.motorized.hookupPos[2]);
			trailerHookupOffset = RotationSystem.getRotatedPoint(trailerHookupOffset, rotationPitch, rotationYaw, rotationRoll);
			
			//Move the trailer to have the hitch align with the hookup.
			motionX = tractorHitchOffset.x - trailerHookupOffset.x;
			motionY = tractorHitchOffset.y - trailerHookupOffset.y;
			motionZ = tractorHitchOffset.z - trailerHookupOffset.z;
			
			//Normalize the hitch and hookpup vectors for yaw changes.
			//Get the angle between them to get the dot product and find out how much yaw we need to add/subtract.
			tractorHitchOffset.normalize();
			trailerHookupOffset.normalize();
			motionYaw = velocity > 0 ? (float) (Math.signum(tractorHitchOffset.crossProduct(trailerHookupOffset).y)*Math.toDegrees(Math.acos(Math.min(1.0D, tractorHitchOffset.dotProduct(trailerHookupOffset))))) : 0;
			
			//If our rear wheels aren't on the ground, pitch upwards to make them be so.
			//Pitch speed is determined by elevation difference of our rear wheels and wheels of what is towing us.
			motionPitch = groundedGroundDevices.isEmpty() ? -(float)(Math.min(Math.max(Math.abs((((towedByVehicle.rearLeftGroundDeviceBox.currentBox.minY + towedByVehicle.rearRightGroundDeviceBox.currentBox.minY)/2) - ((rearLeftGroundDeviceBox.currentBox.minY + rearRightGroundDeviceBox.currentBox.minY)/2)) * 2),0.1),1.0)) : 0;
			
			
			//We apply this angle as the motionYaw, which get applied quicker or slower depending on the trailer size.
			//If we are in the air, pitch up to get our devices on the ground. 
			//Finally, we Match our tower's roll, as that's just a value we generally want to keep.  Hills of course can change this.
			if(Float.isNaN(motionYaw)){
				System.out.println("NAN");
				System.out.println(tractorHitchOffset.dotProduct(trailerHookupOffset));
				System.out.println(Math.toDegrees(Math.acos(tractorHitchOffset.dotProduct(trailerHookupOffset))));
				System.out.println(Math.signum(tractorHitchOffset.crossProduct(trailerHookupOffset).y));
			}
			
			//Try to match our parent's roll.
			motionRoll = (towedByVehicle.rotationRoll - rotationRoll)/10;			
			*/
			
			///START OF OLD CODE.
			
			//We use a second hitchPos here to allow us to calculate the yaw angle we need to apply.
			//If we don't, the vehicle has no clue of the orientation of the towed vehicle hitch and gets all jittery.
			//This is because when the hitch and the hookup are at the same point, the dot product returns floating-point errors.
			Point3d hookupOffset = new Point3d(definition.motorized.hookupPos[0], definition.motorized.hookupPos[1], definition.motorized.hookupPos[2]);
			Point3d hookupPos = RotationSystem.getRotatedPoint(hookupOffset, rotationPitch, rotationYaw, rotationRoll).add(positionVector);
			Point3d hitchOffset = new Point3d(towedByVehicle.definition.motorized.hitchPos[0], towedByVehicle.definition.motorized.hitchPos[1], towedByVehicle.definition.motorized.hitchPos[2]);
			Point3d hitchOffset2 = new Point3d(towedByVehicle.definition.motorized.hitchPos[0], towedByVehicle.definition.motorized.hitchPos[1], towedByVehicle.definition.motorized.hitchPos[2] + 0.5);
			Point3d hitchPos = RotationSystem.getRotatedPoint(hitchOffset, towedByVehicle.rotationPitch, towedByVehicle.rotationYaw, towedByVehicle.rotationRoll).add(towedByVehicle.positionVector);
			Point3d hitchPos2 = RotationSystem.getRotatedPoint(hitchOffset2, towedByVehicle.rotationPitch, towedByVehicle.rotationYaw, towedByVehicle.rotationRoll).add(towedByVehicle.positionVector);
			
			Point3d xzPlaneDelta = new Point3d(hitchPos2.x - hookupPos.x, 0, hitchPos2.z - hookupPos.z).normalize();
			Point3d xzPlaneHeading = new Point3d(headingVector.x, 0, headingVector.z).normalize();
			double towingDeltaYaw = Math.toDegrees(Math.acos(Math.min(Math.abs(xzPlaneDelta.dotProduct(xzPlaneHeading)), 1)));
			if(xzPlaneDelta.crossProduct(xzPlaneHeading).y < 0){
				towingDeltaYaw *= -1;
			}
			
			//Don't apply yaw if we aren't moving. Apply Yaw in proportion to trailer length
			motionYaw = velocity > 0 ? (float) (towingDeltaYaw/(2*Math.abs(definition.motorized.hookupPos[2]))) : 0;
			//If we are in the air, pitch up to get our devices on the ground. Pitching speed is determined by elevation difference of rear wheels.
			motionPitch = groundedGroundDevices.isEmpty() ? -(float)(Math.min(Math.max(Math.abs((((towedByVehicle.rearLeftGroundDeviceBox.currentBox.minY + towedByVehicle.rearRightGroundDeviceBox.currentBox.minY)/2) - ((rearLeftGroundDeviceBox.currentBox.minY + rearRightGroundDeviceBox.currentBox.minY)/2)) * 2),0.1),1.0)) : 0;
			//Match our tower's roll.
			motionRoll = (towedByVehicle.rotationRoll - rotationRoll)/10;
			
			//Now set the motions.
			motionX = hitchPos.x - hookupPos.x;
			motionY = hitchPos.y - hookupPos.y;
			motionZ = hitchPos.z - hookupPos.z;
		}
	}
	
	@Override
	protected void dampenControlSurfaces(){
		if(cruiseControl){
			if(velocity < cruiseControlSpeed){
				if(throttle < 100){
					WrapperNetwork.sendToClientsTracking(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 1, (byte) 0), this);
					++throttle;
				}
			}else if(velocity > cruiseControlSpeed){
				if(throttle > 0){
					WrapperNetwork.sendToClientsTracking(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.THROTTLE, (short) -1, (byte) 0), this);
					--throttle;
				}
			}
		}
		
		if(autopilot){
			if(-rotationRoll > aileronTrim + 1){
				WrapperNetwork.sendToClientsTracking(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, true), this);
				++aileronTrim;
			}else if(-rotationRoll < aileronTrim - 1){
				WrapperNetwork.sendToClientsTracking(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_ROLL, false), this);
				--aileronTrim;
			}
			//If we are not flying at a steady elevation, angle the elevator to compensate
			if(-motionY*100 > elevatorTrim + 1){
				WrapperNetwork.sendToClientsTracking(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, true), this);
				++elevatorTrim;
			}else if(-motionY*100 < elevatorTrim - 1){
				WrapperNetwork.sendToClientsTracking(new PacketVehicleControlDigital(this, PacketVehicleControlDigital.Controls.TRIM_PITCH, false), this);
				--elevatorTrim;
			}
		}
		
		if(aileronCooldown==0){
			if(aileronAngle != 0){
				if(aileronAngle < AILERON_DAMPEN_RATE && aileronAngle > -AILERON_DAMPEN_RATE){
					WrapperNetwork.sendToClientsTracking(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.AILERON, (short) -aileronAngle, (byte) 0), this);
					aileronAngle = 0;
				}else{
					WrapperNetwork.sendToClientsTracking(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.AILERON, aileronAngle < 0 ? AILERON_DAMPEN_RATE : -AILERON_DAMPEN_RATE, (byte) 0), this);
					aileronAngle += aileronAngle < 0 ? AILERON_DAMPEN_RATE : -AILERON_DAMPEN_RATE;
				}
			}
		}else{
			--aileronCooldown;
		}
		
		if(elevatorCooldown==0){
			if(elevatorAngle != 0){
				if(elevatorAngle < ELEVATOR_DAMPEN_RATE && elevatorAngle > -ELEVATOR_DAMPEN_RATE){
					WrapperNetwork.sendToClientsTracking(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.ELEVATOR, (short) -elevatorAngle, (byte) 0), this);
					elevatorAngle = 0;
				}else{
					WrapperNetwork.sendToClientsTracking(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.ELEVATOR, elevatorAngle < 0 ? ELEVATOR_DAMPEN_RATE : -ELEVATOR_DAMPEN_RATE, (byte) 0), this);
					elevatorAngle += elevatorAngle < 0 ? ELEVATOR_DAMPEN_RATE : -ELEVATOR_DAMPEN_RATE;
				}
			}
		}else{
			--elevatorCooldown;
		}
		
		if(rudderCooldown==0){
			if(rudderAngle != 0){
				if(rudderAngle < RUDDER_DAMPEN_RATE && rudderAngle > -RUDDER_DAMPEN_RATE){
					WrapperNetwork.sendToClientsTracking(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.RUDDER, (short) -rudderAngle, (byte) 0), this);
					rudderAngle = 0;
				}else{
					WrapperNetwork.sendToClientsTracking(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.RUDDER, rudderAngle < 0 ? RUDDER_DAMPEN_RATE : -RUDDER_DAMPEN_RATE, (byte) 0), this);
					rudderAngle += rudderAngle < 0 ? RUDDER_DAMPEN_RATE : -RUDDER_DAMPEN_RATE;
				}
			}
		}else{
			--rudderCooldown;
		}
	}
	
	@Override
	public float getSteerAngle(){
		return -rudderAngle/10F;
	}
	
	protected static double getLiftCoeff(double angleOfAttack, double maxLiftCoeff){
		if(Math.abs(angleOfAttack) <= 15*1.25){
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
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.aileronAngle=tagCompound.getShort("aileronAngle");
		this.elevatorAngle=tagCompound.getShort("elevatorAngle");
		this.rudderAngle=tagCompound.getShort("rudderAngle");
		this.flapDesiredAngle=tagCompound.getShort("flapDesiredAngle");
		this.flapCurrentAngle=tagCompound.getShort("flapCurrentAngle");
		this.aileronTrim=tagCompound.getShort("aileronTrim");
		this.elevatorTrim=tagCompound.getShort("elevatorTrim");
		this.rudderTrim=tagCompound.getShort("rudderTrim");
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setShort("aileronAngle", this.aileronAngle);
		tagCompound.setShort("elevatorAngle", this.elevatorAngle);
		tagCompound.setShort("rudderAngle", this.rudderAngle);
		tagCompound.setShort("flapDesiredAngle", this.flapDesiredAngle);
		tagCompound.setShort("flapCurrentAngle", this.flapCurrentAngle);
		tagCompound.setShort("aileronTrim", this.aileronTrim);
		tagCompound.setShort("elevatorTrim", this.elevatorTrim);
		tagCompound.setShort("rudderTrim", this.rudderTrim);
		return tagCompound;
	}
}
