package minecrafttransportsimulator.vehicles.main;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;
import minecrafttransportsimulator.wrappers.WrapperNetwork;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**This class adds control surfaces common to most airborne vehicles.
 * It also adds a few helper functions to be used with physics systems.
 * Note that function of the control surfaces and the buttons assigned
 * to them are left to the handling of the sub-classes of this class
 * and the ControlSystem.
 * 
 * @author don_bruce
 */
public class EntityVehicleF_Air extends EntityVehicleE_Powered{
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
	
	//Internal states.
	public double trackAngle;
	private double currentWingArea;
	
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

			
	public EntityVehicleF_Air(World world){
		super(world);
	}
	
	public EntityVehicleF_Air(World world, float posX, float posY, float posZ, float rotation, JSONVehicle definition){
		super(world, posX, posY, posZ, rotation, definition);
	}
	
	@Override
	protected void getForcesAndMotions(){
		//Set moments.
		momentRoll = (float) (definition.general.emptyMass*(1.5F+(fuel/10000F)));
		momentPitch = (float) (2*currentMass);
		momentYaw = (float) (3*currentMass);
		
		//Set angles and coefficients.
		trackAngle = -Math.toDegrees(Math.asin(verticalVector.dotProduct(velocityVector)));
		wingLiftCoeff = getLiftCoeff(trackAngle, 2 + flapCurrentAngle/350F);
		aileronLiftCoeff = getLiftCoeff((aileronAngle + aileronTrim)/10F, 2);
		elevatorLiftCoeff = getLiftCoeff(-2.5 + trackAngle - (elevatorAngle + elevatorTrim)/10F, 2);
		rudderLiftCoeff = getLiftCoeff((rudderAngle + rudderTrim)/10F + Math.toDegrees(Math.asin(sideVector.dotProduct(velocityVector))), 2);
		dragCoeff = 0.0004F*Math.pow(trackAngle, 2) + 0.03F;
		
		//Adjust flaps to current setting.  If no flaps are present, then this will never change.
		if(flapCurrentAngle < flapDesiredAngle){
			++flapCurrentAngle;
		}else if(flapCurrentAngle > flapDesiredAngle){
			--flapCurrentAngle;
		}
		
		//Blimps are turned with rudders, not ailerons.  This puts the keys at an odd location.  To compensate, 
		//we set the rudder to the aileron if the aileron is greater or less than the rudder.  That way no matter 
		//which key is pressed, they both activate the rudder for turning.
		//Blimps also have more drag than planes, so we 
		if(definition.blimp != null && ((aileronAngle < 0 && aileronAngle < rudderAngle) || (aileronAngle > 0 && aileronAngle > rudderAngle))){
			rudderAngle = aileronAngle;
			rudderCooldown = aileronCooldown;
		}
		
		//If we are a plane, set our current wing area.
		if(definition.plane != null){
			currentWingArea = definition.plane.wingArea + definition.plane.wingArea*flapCurrentAngle/250F;
		}
		
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
		if(definition.blimp != null){
			dragForce = 0.5F*airDensity*velocity*velocity*definition.blimp.crossSectionalArea*dragCoeff;		
			rudderForce = 0.5F*airDensity*velocity*velocity*definition.blimp.rudderArea*rudderLiftCoeff;
			//Blimps aren't affected by gravity.
			gravitationalForce = 0;
			
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
			dragForce = 0.5F*airDensity*velocity*velocity*currentWingArea*(dragCoeff + wingLiftCoeff*wingLiftCoeff/(Math.PI*definition.plane.wingSpan*definition.plane.wingSpan/currentWingArea*0.8));
			wingForce = 0.5F*airDensity*velocity*velocity*currentWingArea*wingLiftCoeff;
			aileronForce = 0.5F*airDensity*velocity*velocity*definition.plane.aileronArea*aileronLiftCoeff;
			elevatorForce = 0.5F*airDensity*velocity*velocity*definition.plane.elevatorArea*elevatorLiftCoeff;			
			rudderForce = 0.5F*airDensity*velocity*velocity*definition.plane.rudderArea*rudderLiftCoeff;
			
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
		}else{
			dragForce = 0.5F*airDensity*velocity*velocity*100F*dragCoeff;
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
		double pitchDirectionFactor = Math.abs(rotationRoll%360);
		pitchDirectionFactor = pitchDirectionFactor < 90 || pitchDirectionFactor > 270 ? 1.0D : -1.0D;
		totalTorque.set(elevatorTorque, rudderTorque, aileronTorque).add(thrustTorque).multiply(180D/Math.PI);
		motionRoll = (float) totalTorque.z/momentRoll;
		motionPitch = (float) (pitchDirectionFactor*(1-Math.abs(sideVector.y))*totalTorque.x - sideVector.y*totalTorque.y)/momentPitch;
		motionYaw = (float) (sideVector.y*totalTorque.x - verticalVector.y*-totalTorque.y)/momentYaw;
	}
	
	@Override
	protected void dampenControlSurfaces(){
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
