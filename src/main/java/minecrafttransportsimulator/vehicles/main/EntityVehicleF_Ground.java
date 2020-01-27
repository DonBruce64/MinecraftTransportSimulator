package minecrafttransportsimulator.vehicles.main;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.packets.control.SteeringPacket;
import minecrafttransportsimulator.systems.RotationSystem;
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
	
	//Variables used for towed logic.
	private double deltaYaw;
	private Vec3d hookupOffset;
	private Vec3d hookupPos;
	private Vec3d hitchOffset;
	private Vec3d hitchOffset2;
	private Vec3d hitchPos;
	private Vec3d hitchPos2;
	private Vec3d xzPlaneDelta;
	private Vec3d xzPlaneHeading;
	
	public EntityVehicleF_Ground towedVehicle;
	public EntityVehicleF_Ground towedByVehicle;
	public byte towingAngle;
	
	public EntityVehicleF_Ground(World world){
		super(world);
	}
	
	public EntityVehicleF_Ground(World world, float posX, float posY, float posZ, float rotation, JSONVehicle definition){
		super(world, posX, posY, posZ, rotation, definition);
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
	public boolean isLightOn(LightTypes light){
		return definition.motorized.isTrailer && towedByVehicle != null ? towedByVehicle.isLightOn(light) : super.isLightOn(light);
	}
	
	@Override
	protected void getBasicProperties(){
		velocityVec = new Vec3d(motionX, motionY, motionZ);
		velocity = velocityVec.dotProduct(headingVec);
		velocityVec = velocityVec.normalize();
		
		if(towedByVehicle != null){
			if(towingAngle < 30){
				++towingAngle;
			}
		}else{
			if(towingAngle > 0){
				--towingAngle;
			}
		}
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
		
		//If we are linked, move us with the other vehicle.
		//Otherwise, do our own logic for movement.
		if(towedByVehicle != null){
			//Check to make sure vehicle isn't dead for some reason.
			if(towedByVehicle.isDead){
				towedByVehicle = null;
			}else{
				//We use a second hitchPos here to allow us to calculate the yaw angle we need to apply.
				//If we don't, the vehicle has no clue of the orientation of the towed vehicle hitch and gets all jittery.
				//This is because when the hitch and the hookup are at the same point, the dot product returns floating-point errors.
				hookupOffset = new Vec3d(definition.motorized.hookupPos[0], definition.motorized.hookupPos[1], definition.motorized.hookupPos[2]);
				hookupPos = RotationSystem.getRotatedPoint(hookupOffset, rotationPitch, rotationYaw, rotationRoll).add(getPositionVector());
				hitchOffset = new Vec3d(towedByVehicle.definition.motorized.hitchPos[0], towedByVehicle.definition.motorized.hitchPos[1], towedByVehicle.definition.motorized.hitchPos[2]);
				hitchOffset2 = new Vec3d(towedByVehicle.definition.motorized.hitchPos[0], towedByVehicle.definition.motorized.hitchPos[1], towedByVehicle.definition.motorized.hitchPos[2] + 0.5);
				hitchPos = RotationSystem.getRotatedPoint(hitchOffset, towedByVehicle.rotationPitch, towedByVehicle.rotationYaw, towedByVehicle.rotationRoll).add(towedByVehicle.getPositionVector());
				hitchPos2 = RotationSystem.getRotatedPoint(hitchOffset2, towedByVehicle.rotationPitch, towedByVehicle.rotationYaw, towedByVehicle.rotationRoll).add(towedByVehicle.getPositionVector());
				
				xzPlaneDelta = new Vec3d(hitchPos2.x - hookupPos.x, 0, hitchPos2.z - hookupPos.z).normalize();
				xzPlaneHeading = new Vec3d(headingVec.x, 0, headingVec.z).normalize();
				deltaYaw = Math.toDegrees(Math.acos(Math.min(Math.abs(xzPlaneDelta.dotProduct(xzPlaneHeading)), 1)));
				if(xzPlaneDelta.crossProduct(xzPlaneHeading).y < 0){
					deltaYaw *= -1;
				}
				
				//Don't apply yaw if we aren't moving.
				motionYaw = velocity > 0 ? (float) (deltaYaw/10) : 0;
				//If we are in the air, pitch up to get our devices on the ground.
				motionPitch = groundedGroundDevices.isEmpty() ? -1F : 0;
				//Match our tower's roll.
				motionRoll = (towedByVehicle.rotationRoll - rotationRoll)/10;
				
				motionX = hitchPos.x - hookupPos.x;
				motionY = hitchPos.y - hookupPos.y;
				motionZ = hitchPos.z - hookupPos.z;
				return;
			}
		}
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