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
	private final PartGroundDeviceFake fakePart;
	
	public boolean skipAngularCalcs = false;
	public float angularPosition;
	public float angularVelocity;
	
	public APartGroundDevice(EntityMultipartD_Moving multipart, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(multipart, packPart, partName, dataTag);
		
		//This constructor is the super for fake parts.  Intercept these calls and bypass fake part creation.
		//If we are a long ground device, add a fake ground device at the offset to make us
		//have a better contact area.  If we are a fake part calling this as a super constructor,
		//we will be invalid.  Check that to prevent loops.
		if(this.isValid() && this.getLongPartOffset() != 0){
			packPart.pos[2] += this.getLongPartOffset();
			fakePart = new PartGroundDeviceFake(this, packPart, partName, dataTag);
			multipart.addPart(fakePart, false);
			packPart.pos[2] -= this.getLongPartOffset();
		}else{
			fakePart = null;
		}
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		if(this.isOnGround()){	
			if(!multipart.worldObj.isRemote && multipart.velocity > 0.2F){
				List<EntityLivingBase> collidedEntites = multipart.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.getAABBWithOffset(Vec3d.ZERO).expand(0.25F, 0, 0.25F));
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
	
	@Override
	public void removePart(){
		super.removePart();
		if(fakePart != null){
			multipart.removePart(fakePart, false);
		}
	}
	
	public float getFrictionLoss(){
		//0.6 is default slipperiness for blocks.  Anything extra should reduce friction, anything less should increase it.
		BlockPos pos = new BlockPos(partPos.addVector(0, -1, 0));
		return 0.6F - multipart.worldObj.getBlockState(pos).getBlock().slipperiness;
	}
	
	public boolean isOnGround(){
		return isPartCollidingWithBlocks(groundDetectionOffset);
	}
	
	public abstract float getMotiveFriction();
	
	public abstract float getLateralFriction();
	
	public abstract float getLongPartOffset();
	
	public abstract boolean canBeDrivenByEngine();
}
