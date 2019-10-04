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

public final class EntityVehicleF_Plane extends EntityVehicleE_Powered{
	public boolean reverseThrust;
	public short reversePercent;
	
	//Note that angle variable should be divided by 10 to get actual angle.
	public short aileronAngle;
	public short elevatorAngle;
	public short rudderAngle;
	public short flapDesiredAngle;
	public short flapCurrentAngle;
	public short aileronTrim;
	public short elevatorTrim;
	public short rudderTrim;
	
	public short aileronCooldown;
	public short elevatorCooldown;
	public short rudderCooldown;
	
	public double trackAngle;
	public Vec3d verticalVec = Vec3d.ZERO;
	public Vec3d sideVec = Vec3d.ZERO;

	//Internal plane variables
	private float momentRoll;
	private float momentPitch;
	private float momentYaw;
	private double currentWingArea;
	private double dragCoeff;
	private double wingLiftCoeff;
	private double aileronLiftCoeff;
	private double elevatorLiftCoeff;
	private double rudderLiftCoeff;
	private double thrust;
	
	private double thrustForce;//kg*m/ticks^2
	private double dragForce;//kg*m/ticks^2
	private double wingForce;//kg*m/ticks^2
	private double aileronForce;//kg*m/ticks^2
	private double elevatorForce;//kg*m/ticks^2
	private double rudderForce;//kg*m/ticks^2
	private double gravitationalForce;//kg*m/ticks^2
	private double thrustTorque;//kg*m^2/ticks^2
	private double aileronTorque;//kg*m^2/ticks^2
	private double elevatorTorque;//kg*m^2/ticks^2
	private double rudderTorque;//kg*m^2/ticks^2
			
	public EntityVehicleF_Plane(World world){
		super(world);
	}
	
	public EntityVehicleF_Plane(World world, float posX, float posY, float posZ, float rotation, String name){
		super(world, posX, posY, posZ, rotation, name);
	}
	
	@Override
	protected void getBasicProperties(){
		if(reverseThrust && reversePercent < 100){
			++reversePercent;
		}else if(!reverseThrust && reversePercent > 0){
			--reversePercent;
		}
		
		if(flapCurrentAngle < flapDesiredAngle){
			++flapCurrentAngle;
		}else if(flapCurrentAngle > flapDesiredAngle){
			--flapCurrentAngle;
		}
		
		momentRoll = (float) (pack.general.emptyMass*(1.5F+(fuel/10000F)));
		momentPitch = (float) (2*currentMass);
		momentYaw = (float) (3*currentMass);
		currentWingArea = pack.plane.wingArea + pack.plane.wingArea*flapCurrentAngle/250F;
		
		verticalVec = RotationSystem.getRotatedY(rotationPitch, rotationYaw, rotationRoll);
		sideVec = headingVec.crossProduct(verticalVec);
		velocityVec = new Vec3d(motionX, motionY, motionZ);
		velocity = velocityVec.dotProduct(headingVec);
		velocityVec = velocityVec.normalize();
		
		trackAngle = Math.toDegrees(Math.atan2(velocityVec.dotProduct(verticalVec), velocityVec.dotProduct(headingVec)));
		dragCoeff = 0.0004F*Math.pow(trackAngle, 2) + 0.03F;
		wingLiftCoeff = getLiftCoeff(-trackAngle, 2 + flapCurrentAngle/350F);
		aileronLiftCoeff = getLiftCoeff((aileronAngle + aileronTrim)/10F, 2);
		elevatorLiftCoeff = getLiftCoeff(-2.5 - trackAngle - (elevatorAngle + elevatorTrim)/10F, 2);
		rudderLiftCoeff = getLiftCoeff((rudderAngle + rudderTrim)/10F + Math.toDegrees(Math.atan2(velocityVec.dotProduct(sideVec), velocityVec.dotProduct(headingVec))), 2);
	}
	
	@Override
	protected void getForcesAndMotions(){
		thrustForce = thrustTorque = 0;
		for(byte i=0; i<this.getNumberEngineBays(); ++i){
			APartEngine engine = getEngineByNumber(i);
			if(engine != null){
				thrust = engine.getForceOutput();
				thrustForce += thrust;
				thrustTorque += thrust*engine.offset.x;
			}
		}
		
		dragForce = 0.5F*airDensity*velocity*velocity*currentWingArea*(dragCoeff + wingLiftCoeff*wingLiftCoeff/(Math.PI*pack.plane.wingSpan*pack.plane.wingSpan/currentWingArea*0.8));		
		wingForce = 0.5F*airDensity*velocity*velocity*currentWingArea*wingLiftCoeff;
		aileronForce = 0.5F*airDensity*velocity*velocity*pack.plane.wingArea/5F*aileronLiftCoeff;
		elevatorForce = 0.5F*airDensity*velocity*velocity*pack.plane.elevatorArea*elevatorLiftCoeff;			
		rudderForce = 0.5F*airDensity*velocity*velocity*pack.plane.rudderArea*rudderLiftCoeff;
		gravitationalForce = currentMass*(9.8/400);
					
		aileronTorque = aileronForce*pack.plane.wingSpan*0.5F*0.75F;
		elevatorTorque = elevatorForce*pack.plane.tailDistance;
		rudderTorque = rudderForce*pack.plane.tailDistance;
		
		//As a special case, if the plane is pointed upwards and stalling, add a forwards pitch to allow the plane to right itself.
		//This is needed to prevent the plane from getting stuck in a vertical position and crashing.
		if(velocity < 0 && groundedGroundDevices.isEmpty()){
			if(rotationPitch < 0 && rotationPitch >= -120){
				elevatorTorque += 100;
			}
		}
				
		motionX += (headingVec.x*thrustForce - velocityVec.x*dragForce + verticalVec.x*(wingForce + elevatorForce))/currentMass;
		motionZ += (headingVec.z*thrustForce - velocityVec.z*dragForce + verticalVec.z*(wingForce + elevatorForce))/currentMass;
		motionY += (headingVec.y*thrustForce - velocityVec.y*dragForce + verticalVec.y*(wingForce + elevatorForce) - gravitationalForce)/currentMass;
		
		motionRoll = (float) (180/Math.PI*((1-headingVec.y)*aileronTorque)/momentRoll);
		motionPitch = (float) (180/Math.PI*((1-Math.abs(sideVec.y))*elevatorTorque - sideVec.y*(thrustTorque + rudderTorque))/momentPitch);
		motionYaw = (float) (180/Math.PI*(headingVec.y*aileronTorque - verticalVec.y*(-thrustTorque - rudderTorque) + sideVec.y*elevatorTorque)/momentYaw);
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
	
	private double getLiftCoeff(double angleOfAttack, double maxLiftCoeff){
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