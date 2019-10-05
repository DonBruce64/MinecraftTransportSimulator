package minecrafttransportsimulator.vehicles.main;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.control.SteeringPacket;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;


public abstract class EntityVehicleF_Ground extends EntityVehicleE_Powered{	
	//Note that angle variable should be divided by 10 to get actual angle.
	public short steeringAngle;
	public short steeringCooldown;
	
	//Internal variables
	private double forwardForce;//kg*m/ticks^2
	private double dragForce;//kg*m/ticks^2
	private double gravitationalForce;//kg*m/ticks^2
	
	public EntityVehicleF_Ground(World world){
		super(world);
	}
	
	public EntityVehicleF_Ground(World world, float posX, float posY, float posZ, float rotation, String vehicleName){
		super(world, posX, posY, posZ, rotation, vehicleName);
	}
	
	@Override
	protected void getBasicProperties(){
		velocityVec = new Vec3d(motionX, motionY, motionZ);
		velocity = velocityVec.dotProduct(headingVec);
		velocityVec = velocityVec.normalize();
	}
	
	@Override
	protected void getForcesAndMotions(){
		if(getEngineByNumber((byte) 0) != null){
			forwardForce = getEngineByNumber((byte) 0).getForceOutput();
		}else{
			forwardForce = 0;
		}
		
		dragForce = 0.5F*airDensity*velocity*velocity*5.0F*getDragCoefficient();
		gravitationalForce = currentMass*(9.8/400);
				
		motionX += (headingVec.x*forwardForce - velocityVec.x*dragForce)/currentMass;
		motionZ += (headingVec.z*forwardForce - velocityVec.z*dragForce)/currentMass;
		motionY += (headingVec.y*forwardForce - velocityVec.y*dragForce - gravitationalForce)/currentMass;
		
		motionYaw = 0;
		motionPitch = 0;
		motionRoll = 0;
	}
	
	@Override
	protected void dampenControlSurfaces(){
		if(steeringCooldown==0){
			if(steeringAngle != 0){
				MTS.MTSNet.sendToAll(new SteeringPacket(this.getEntityId(), steeringAngle < 0, (short) 0));
				steeringAngle += steeringAngle < 0 ? 20 : -20;
			}
		}else{
			--steeringCooldown;
		}
	}
	
	@Override
	public float getSteerAngle(){
		return -steeringAngle/10F;
	}
	
	protected abstract float getDragCoefficient();
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.steeringAngle=tagCompound.getShort("steeringAngle");
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setShort("steeringAngle", this.steeringAngle);
		return tagCompound;
	}
}