package minecrafttransportsimulator.vehicles.main;

import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import minecrafttransportsimulator.wrappers.WrapperNetwork;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**This class adds control surfaces common to most airborne vehicles.
 * It also adds a few helper functions to be used with physics systems.
 * Note that function of the control surfaces and the buttons assigned
 * to them are left to the handling of the sub-classes of this class
 * and the ControlSystem.
 * 
 * @author don_bruce
 */
public abstract class EntityVehicleF_Air extends EntityVehicleE_Powered{
	//Note that angle variable should be divided by 10 to get actual angle.
	public final short MAX_AILERON_ANGLE = 250;
	public final short AILERON_DAMPEN_RATE = 6;
	public short aileronAngle;
	public short aileronTrim;
	public byte aileronCooldown;
	public boolean autopilot;
	
	public final short MAX_ELEVATOR_ANGLE = 250;
	public final short ELEVATOR_DAMPEN_RATE = 6;
	public short elevatorAngle;
	public short elevatorTrim;
	public byte elevatorCooldown;
	
	public final short MAX_RUDDER_ANGLE = 450;
	public final short RUDDER_DAMPEN_RATE = 20;
	public short rudderAngle;
	public short rudderTrim;
	public byte rudderCooldown;
	
	//Other variables.
	public double trackAngle;
	public Vec3d verticalVec = Vec3d.ZERO;
	public Vec3d sideVec = Vec3d.ZERO;

	//Internal aircraft variables
	protected float momentRoll;
	protected float momentPitch;
	protected float momentYaw;
	protected double aileronLiftCoeff;
	protected double elevatorLiftCoeff;
	protected double rudderLiftCoeff;
	
	protected double thrustForce;//kg*m/ticks^2
	protected double gravitationalForce;//kg*m/ticks^2
	protected double thrustTorque;//kg*m^2/ticks^2

			
	public EntityVehicleF_Air(World world){
		super(world);
	}
	
	public EntityVehicleF_Air(World world, float posX, float posY, float posZ, float rotation, JSONVehicle definition){
		super(world, posX, posY, posZ, rotation, definition);
	}
	
	@Override
	protected void getBasicProperties(){
		momentRoll = (float) (definition.general.emptyMass*(1.5F+(fuel/10000F)));
		momentPitch = (float) (2*currentMass);
		momentYaw = (float) (3*currentMass);
		
		verticalVec = RotationSystem.getRotatedY(rotationPitch, rotationYaw, rotationRoll);
		sideVec = headingVec.crossProduct(verticalVec);
		velocityVec = new Vec3d(motionX, motionY, motionZ);
		velocity = velocityVec.dotProduct(headingVec);
		velocityVec = velocityVec.normalize();
		
		trackAngle = Math.toDegrees(Math.atan2(velocityVec.dotProduct(verticalVec), velocityVec.dotProduct(headingVec)));
		aileronLiftCoeff = getLiftCoeff((aileronAngle + aileronTrim)/10F, 2);
		elevatorLiftCoeff = getLiftCoeff(-2.5 - trackAngle - (elevatorAngle + elevatorTrim)/10F, 2);
		rudderLiftCoeff = getLiftCoeff((rudderAngle + rudderTrim)/10F + Math.toDegrees(Math.atan2(velocityVec.dotProduct(sideVec), velocityVec.dotProduct(headingVec))), 2);
	}
	
	@Override
	protected void getForcesAndMotions(){
		thrustForce = thrustTorque = 0;
		double thrust = 0;
		for(APartEngine engine : engines.values()){
			thrust = engine.getForceOutput();
			thrustForce += thrust;
			thrustTorque += thrust*engine.offset.x;
		}
		gravitationalForce = currentMass*(9.8/400);
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
		tagCompound.setShort("aileronTrim", this.aileronTrim);
		tagCompound.setShort("elevatorTrim", this.elevatorTrim);
		tagCompound.setShort("rudderTrim", this.rudderTrim);
		return tagCompound;
	}
}
