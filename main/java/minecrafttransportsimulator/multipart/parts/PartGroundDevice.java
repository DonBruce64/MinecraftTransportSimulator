package minecrafttransportsimulator.multipart.parts;

import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MultipartAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.DamageSources.DamageSourceWheel;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Car;
import minecrafttransportsimulator.packets.parts.PacketPartGroundDeviceFlat;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.systems.SFXSystem.FXPart;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**Any child that touches the ground should extend this class.
 * It's used to perform ground physics and rendering of wheels, pontoons, and whatever
 * else people may want their vehicles to touch the ground with.
 * 
 * @author don_bruce
 */
public class PartGroundDevice extends APart implements FXPart{
	private static final Vec3d groundDetectionOffset = new Vec3d(0, -0.05F, 0);
	private static final Vec3d mirrorRotation = new Vec3d(0, 180, 0);
	private final PartGroundDeviceFake fakePart;
	private ResourceLocation flatModelLocation;
	
	private boolean isFlat;
	private boolean contactThisTick = false;
	public boolean skipAngularCalcs = false;
	private int ticksCalcsSkipped = 0;

	public float angularPosition;
	public float angularVelocity;
	
	public PartGroundDevice(EntityMultipartD_Moving multipart, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(multipart, packPart, partName, dataTag);
		this.isFlat = dataTag.getBoolean("isFlat");
		
		//This constructor is the super for fake parts.  Intercept these calls and bypass fake part creation.
		//If we are a long ground device, add a fake ground device at the offset to make us
		//have a better contact area.  If we are a fake part calling this as a super constructor,
		//we will be invalid.  Check that to prevent loops.
		if(this.isValid() && pack.groundDevice.isLongPart){
			packPart.pos[2] += pack.groundDevice.extraCollisionBoxOffset;
			fakePart = new PartGroundDeviceFake(this, packPart, partName, dataTag);
			multipart.addPart(fakePart, false);
			packPart.pos[2] -= pack.groundDevice.extraCollisionBoxOffset;
		}else{
			fakePart = null;
		}
	}
	
	@Override
	public void attackPart(DamageSource source, float damage){
		if(this.pack.groundDevice.canBeFlat && !this.isFlat){
			if(source.isExplosion() || Math.random() < 0.1){
				if(!multipart.worldObj.isRemote){
					this.setFlat();
					Vec3d explosionPosition = partPos;
					multipart.worldObj.newExplosion(multipart, explosionPosition.xCoord, explosionPosition.yCoord, explosionPosition.zCoord, 0.25F, false, false);
					MTS.MTSNet.sendToAll(new PacketPartGroundDeviceFlat(this));
				}
			}
		}
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		if(this.isOnGround()){
			if(this.pack.groundDevice.rotatesOnShaft){
				if(angularVelocity/(multipart.velocity/this.getHeight()) < 0.25 && multipart.velocity > 0.3){
					BlockPos blockBelow = new BlockPos(partPos).down();
					if(multipart.worldObj.getBlockState(blockBelow).getBlockHardness(multipart.worldObj, blockBelow) >= 1.5){
						contactThisTick = true;
					}
				}
				
				if(!skipAngularCalcs){
					angularVelocity = (float) (multipart.velocity/this.getHeight());
					if(ticksCalcsSkipped > 0 && pack.groundDevice.canBeFlat && !isFlat){
						--ticksCalcsSkipped;
					}
				}else if(pack.groundDevice.canBeFlat && !isFlat){
					++ticksCalcsSkipped;
					if(Math.random()*50000 < ticksCalcsSkipped){
						if(!multipart.worldObj.isRemote){
							this.setFlat();
							Vec3d explosionPosition = partPos;
							multipart.worldObj.newExplosion(multipart, explosionPosition.xCoord, explosionPosition.yCoord, explosionPosition.zCoord, 0.25F, false, false);
							MTS.MTSNet.sendToAll(new PacketPartGroundDeviceFlat(this));
						}
					}
				}
			}
			
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
		if(this.fakePart != null){
			multipart.removePart(fakePart, false);
		}
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = new NBTTagCompound();
		dataTag.setBoolean("isFlat", this.isFlat);
		return dataTag;
	}
	
	@Override
	public float getWidth(){
		return this.pack.groundDevice.thickness;
	}
	
	@Override
	public float getHeight(){
		return this.isFlat ? this.pack.groundDevice.flatDiameter : this.pack.groundDevice.diameter;
	}
	
	@Override
	public Item getItemForPart(){
		return this.isFlat ? null : super.getItemForPart();
	}
	
	@Override
	public ResourceLocation getModelLocation(){
		if(this.isFlat){
			if(flatModelLocation == null){
				if(pack.general.modelName != null){
					flatModelLocation = new ResourceLocation(partName.substring(0, partName.indexOf(':')), "objmodels/parts/" + pack.general.modelName + "_flat.obj");
				}else{
					flatModelLocation = new ResourceLocation(partName.substring(0, partName.indexOf(':')), "objmodels/parts/" + partName.substring(partName.indexOf(':') + 1) + "_flat.obj");
				}
			}
			return flatModelLocation;
		}else{
			return super.getModelLocation();
		}
	}
	
	@Override
	public Vec3d getActionRotation(float partialTicks){
		if(this.pack.groundDevice.rotatesOnShaft){
			return new Vec3d(this.angularPosition + this.angularVelocity*partialTicks, 0, 0);
		}else{
			return Vec3d.ZERO;
		}
	}
	
	@Override
	public boolean isPartCollidingWithBlocks(Vec3d collisionOffset){
		if(super.isPartCollidingWithBlocks(collisionOffset)){
			return true;
    	}else if(!this.pack.groundDevice.canFloat){
    		return false;
    	}else{
    		MultipartAxisAlignedBB collisionBox = this.getAABBWithOffset(collisionOffset);
			int minX = (int) Math.floor(collisionBox.minX);
	    	int maxX = (int) Math.floor(collisionBox.maxX + 1.0D);
	    	int minY = (int) Math.floor(collisionBox.minY);
	    	int maxY = (int) Math.floor(collisionBox.maxY + 1.0D);
	    	int minZ = (int) Math.floor(collisionBox.minZ);
	    	int maxZ = (int) Math.floor(collisionBox.maxZ + 1.0D);
	    	
	    	for(int i = minX; i < maxX; ++i){
	    		for(int j = minY; j < maxY; ++j){
	    			for(int k = minZ; k < maxZ; ++k){
	    				BlockPos checkPos = new BlockPos(i, j, k);
	    				if(multipart.worldObj.isBlockLoaded(checkPos)){
		    				if(multipart.worldObj.getBlockState(checkPos).getMaterial().isLiquid()){
		    					return true;
		    				}
	    				}
	    			}
	    		}
	    	}
	    	return false;
    	}
    }
	
	public float getMotiveFriction(){
		return !this.isFlat ? this.pack.groundDevice.motiveFriction : this.pack.groundDevice.motiveFriction/10F;
	}
	
	public float getLateralFriction(){
		return !this.isFlat ? this.pack.groundDevice.lateralFriction : this.pack.groundDevice.lateralFriction/10F;
	}
	
	public float getFrictionLoss(){
		//0.6 is default slipperiness for blocks.  Anything extra should reduce friction, anything less should increase it.
		BlockPos pos = new BlockPos(partPos.addVector(0, -1, 0));
		return 0.6F - multipart.worldObj.getBlockState(pos).getBlock().slipperiness;
	}
	
	public boolean isOnGround(){
		return isPartCollidingWithBlocks(groundDetectionOffset);
	}
	
	public void setFlat(){
		this.isFlat = true;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void spawnParticles(){
		if(contactThisTick){
			for(byte i=0; i<4; ++i){
				Minecraft.getMinecraft().effectRenderer.addEffect(new SFXSystem.WhiteSmokeFX(multipart.worldObj, partPos.xCoord, partPos.yCoord, partPos.zCoord, Math.random()*0.10 - 0.05, 0.15, Math.random()*0.10 - 0.05));
			}
			MTS.proxy.playSound(this.partPos, MTS.MODID + ":" + "wheel_striking", 1, 1);
			contactThisTick = false;
		}
		if(skipAngularCalcs){
			for(byte i=0; i<4; ++i){
				Minecraft.getMinecraft().effectRenderer.addEffect(new SFXSystem.WhiteSmokeFX(multipart.worldObj, partPos.xCoord, partPos.yCoord, partPos.zCoord, Math.random()*0.10 - 0.05, 0.15, Math.random()*0.10 - 0.05));
			}
		}
	}
}
