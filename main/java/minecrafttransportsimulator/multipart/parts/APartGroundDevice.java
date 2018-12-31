package minecrafttransportsimulator.multipart.parts;

import java.util.List;

import minecrafttransportsimulator.dataclasses.DamageSources.DamageSourceWheel;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Car;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**Any child that touches the ground should extend this class.
 * It's used to perform ground physics and rendering of wheels, pontoons, and whatever
 * else people may want their vehicles to touch the ground with.
 * 
 * @author don_bruce
 */
public abstract class APartGroundDevice extends APart{
	private static final Vec3d groundDetectionOffset = new Vec3d(0, -0.05F, 0);
	private static final Vec3d mirrorRotation = new Vec3d(0, 180, 0);
	
	public boolean skipAngularCalcs = false;
	public float angularPosition;
	public float angularVelocity;
	
	public APartGroundDevice(EntityMultipartD_Moving multipart, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(multipart, packPart, partName, dataTag);
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		if(this.isOnGround()){	
			if(!multipart.worldObj.isRemote && multipart.velocity > 0.2F){
				List<EntityLivingBase> collidedEntites = multipart.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.getPartBox());
				if(!collidedEntites.isEmpty()){
					Entity attacker = null;
					for(Entity passenger : multipart.getPassengers()){
						PartSeat seat = multipart.getSeatForRider(passenger);
						if(seat.isController){
							attacker = passenger;
							break;
						}
					}
					for(int i=0; i < collidedEntites.size(); ++i){
						if(!this.multipart.isPassenger(collidedEntites.get(i))){
							collidedEntites.get(i).attackEntityFrom(new DamageSourceWheel(attacker), (float) (ConfigSystem.getDoubleConfig("WheelDamageFactor")*multipart.velocity*multipart.currentMass/1000F));
						}
					}
				}
			}
		}else if(!(multipart instanceof EntityMultipartF_Car)){
			if(multipart.brakeOn || multipart.parkingBrakeOn){
				angularVelocity = 0;
			}else if(angularVelocity>0){
				angularVelocity = (float) Math.max(angularVelocity - 0.05, 0);
			}
		}
		angularPosition += Math.toDegrees(angularVelocity);
	}
	
	public float getFrictionLoss(){
		//0.6 is default slipperiness for blocks.  Anything extra should reduce friction, anything less should increase it.
		BlockPos pos = new BlockPos(partPos.addVector(0, -1, 0));
		return 0.6F - multipart.worldObj.getBlockState(pos).getBlock().slipperiness;
	}
	
	public boolean isOnGround(){
		//TODO this needs to be re-done to not use static positioning.
		//return isPartCollidingWithBlocks(groundDetectionOffset);
		return false;
	}
	
	public abstract float getMotiveFriction();
	
	public abstract float getLateralFriction();
		
	public abstract boolean canBeDrivenByEngine();
}
