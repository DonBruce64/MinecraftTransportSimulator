package minecrafttransportsimulator.vehicles.main;

import minecrafttransportsimulator.jsondefs.JSONVehicle;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class EntityVehicleG_Plane extends EntityVehicleF_Air{
	
	//Note that angle variable should be divided by 10 to get actual angle.
	public short flapDesiredAngle;
	public short flapCurrentAngle;
	
	//Internal plane variables
	private double currentWingArea;
	private double dragCoeff;
	private double wingLiftCoeff;
	
	private double dragForce;//kg*m/ticks^2
	private double wingForce;//kg*m/ticks^2
	private double aileronForce;//kg*m/ticks^2
	private double elevatorForce;//kg*m/ticks^2
	private double rudderForce;//kg*m/ticks^2
	private double aileronTorque;//kg*m^2/ticks^2
	private double elevatorTorque;//kg*m^2/ticks^2
	private double rudderTorque;//kg*m^2/ticks^2
			
	public EntityVehicleG_Plane(World world){
		super(world);
	}
	
	public EntityVehicleG_Plane(World world, float posX, float posY, float posZ, float rotation, JSONVehicle definition){
		super(world, posX, posY, posZ, rotation, definition);
	}
	
	@Override
	protected void getBasicProperties(){
		super.getBasicProperties();
		if(flapCurrentAngle < flapDesiredAngle){
			++flapCurrentAngle;
		}else if(flapCurrentAngle > flapDesiredAngle){
			--flapCurrentAngle;
		}
		
		currentWingArea = definition.plane.wingArea + definition.plane.wingArea*flapCurrentAngle/250F;
		
		dragCoeff = 0.0004F*Math.pow(trackAngle, 2) + 0.03F;
		wingLiftCoeff = getLiftCoeff(-trackAngle, 2 + flapCurrentAngle/350F);
	}
	
	@Override
	protected void getForcesAndMotions(){
		super.getForcesAndMotions();
		dragForce = 0.5F*airDensity*velocity*velocity*currentWingArea*(dragCoeff + wingLiftCoeff*wingLiftCoeff/(Math.PI*definition.plane.wingSpan*definition.plane.wingSpan/currentWingArea*0.8));		
		wingForce = 0.5F*airDensity*velocity*velocity*currentWingArea*wingLiftCoeff;
		aileronForce = 0.5F*airDensity*velocity*velocity*definition.plane.wingArea/5F*aileronLiftCoeff;
		elevatorForce = 0.5F*airDensity*velocity*velocity*definition.plane.elevatorArea*elevatorLiftCoeff;			
		rudderForce = 0.5F*airDensity*velocity*velocity*definition.plane.rudderArea*rudderLiftCoeff;
					
		aileronTorque = aileronForce*definition.plane.wingSpan*0.5F*0.75F;
		elevatorTorque = elevatorForce*definition.plane.tailDistance;
		rudderTorque = rudderForce*definition.plane.tailDistance;
		
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
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.flapDesiredAngle=tagCompound.getShort("flapDesiredAngle");
		this.flapCurrentAngle=tagCompound.getShort("flapCurrentAngle");
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setShort("flapDesiredAngle", this.flapDesiredAngle);
		tagCompound.setShort("flapCurrentAngle", this.flapCurrentAngle);
		return tagCompound;
	}
}