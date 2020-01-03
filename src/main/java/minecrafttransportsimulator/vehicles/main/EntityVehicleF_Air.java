package minecrafttransportsimulator.vehicles.main;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.control.AileronPacket;
import minecrafttransportsimulator.packets.control.ElevatorPacket;
import minecrafttransportsimulator.packets.control.RudderPacket;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
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
	public boolean reverseThrust;
	public short reversePercent;
	
	//Note that angle variable should be divided by 10 to get actual angle.
	public short aileronAngle;
	public short elevatorAngle;
	public short rudderAngle;
	public short aileronTrim;
	public short elevatorTrim;
	public short rudderTrim;
	
	public short aileronCooldown;
	public short elevatorCooldown;
	public short rudderCooldown;
	
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
	
	public EntityVehicleF_Air(World world, float posX, float posY, float posZ, float rotation, String name){
		super(world, posX, posY, posZ, rotation, name);
	}
	
	@Override
	protected void getBasicProperties(){
		if(reverseThrust && reversePercent < 20){
			++reversePercent;
		}else if(!reverseThrust && reversePercent > 0){
			--reversePercent;
		}
		
		momentRoll = (float) (pack.general.emptyMass*(1.5F+(fuel/10000F)));
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
		for(byte i=0; i<this.getNumberEngineBays(); ++i){
			APartEngine engine = getEngineByNumber(i);
			if(engine != null){
				thrust = engine.getForceOutput();
				thrustForce += thrust;
				thrustTorque += thrust*engine.offset.x;
			}
		}
		gravitationalForce = currentMass*(9.8/400);
	}

	@Override
	protected void dampenControlSurfaces(){
		if(aileronCooldown==0){
			if(aileronAngle != 0){
				MTS.MTSNet.sendToAll(new AileronPacket(this.getEntityId(), aileronAngle < 0, (short) 0));
				aileronAngle += aileronAngle < 0 ? 6 : -6;
			}
		}else{
			--aileronCooldown;
		}
		if(elevatorCooldown==0){
			if(elevatorAngle != 0){
				MTS.MTSNet.sendToAll(new ElevatorPacket(this.getEntityId(), elevatorAngle < 0, (short) 0));
				elevatorAngle += elevatorAngle < 0 ? 6 : -6;
			}
		}else{
			--elevatorCooldown;
		}
		if(rudderCooldown==0){
			if(rudderAngle != 0){
				MTS.MTSNet.sendToAll(new RudderPacket(this.getEntityId(), rudderAngle < 0, (short) 0));
				rudderAngle += rudderAngle < 0 ? 6 : -6;
			}
		}else{
			--rudderCooldown;
		}
	}
	
	@Override
	public float getSteerAngle(){
		return -rudderAngle/10F;
	}
	
	protected double getLiftCoeff(double angleOfAttack, double maxLiftCoeff){
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