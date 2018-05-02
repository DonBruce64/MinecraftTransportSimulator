package minecrafttransportsimulator.multipart.parts;

import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSDamageSources.DamageSourceWheel;
import minecrafttransportsimulator.entities.core.EntityMultipartA_Base;
import minecrafttransportsimulator.entities.main.EntityCar;
import minecrafttransportsimulator.entities.parts.EntityMultipartChild;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.packets.parts.PacketFlatGroundDevice;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**Any child that touches the ground should extend this class.
 * It's used to perform ground physics and rendering.
 * 
 * @author don_bruce
 */
public final class PartGroundDevice extends AMultipartPart{
	private boolean isFlat;
	private float angularPosition;
	private float angularVelocity;
	private boolean contactThisTick = false;
	
	private static final Vec3d groundDetectionOffset = new Vec3d(0, -0.05F, 0);
	
	public PartGroundDevice(EntityMultipartA_Base multipart, Vec3d offset, boolean isController, boolean turnsWithSteer, String partName, NBTTagCompound dataTag){
		super(multipart, offset, isController, turnsWithSteer, partName, dataTag);
	}
	
	@Override
	public void attackPart(DamageSource source, float damage){
		if(this.packInfo.groundDevice.canBeFlat && !this.isFlat){
			if(source.isExplosion() || Math.random() < 0.1){
				if(!multipart.worldObj.isRemote){
					this.setFlat();
					Vec3d explosionPosition = multipart.getPositionVector().add(this.offset);
					multipart.worldObj.newExplosion(multipart, explosionPosition.xCoord, explosionPosition.yCoord, explosionPosition.zCoord, 0.25F, false, false);
					MTS.MTSNet.sendToAll(new PacketFlatGroundDevice(this));
				}
			}
		}
	}
	
	@Override
	public void updatePart(){
		if(this.isOnGround()){
			if(this.packInfo.groundDevice.rotatesOnShaft){
				if(angularVelocity/(multipart.velocity/this.getHeight()) < 0.25 && multipart.velocity > 0.3){
					BlockPos blockBelow = new BlockPos(multipart.getPositionVector().add(this.offset)).down();
					if(multipart.worldObj.getBlockState(blockBelow).getBlockHardness(multipart.worldObj, blockBelow) >= 1.5){
						contactThisTick = true;
					}
				}
				angularVelocity = (float) (multipart.velocity/this.getHeight());
			}
			
			if(!multipart.worldObj.isRemote && multipart.velocity > 0.2F){
				List<EntityLivingBase> collidedEntites = multipart.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.getAABBWithOffset(Vec3d.ZERO).expand(0.25F, 0, 0.25F));
				if(!collidedEntites.isEmpty()){
					Entity attacker = null;
					for(EntityMultipartChild child : parent.getChildren()){
						if(child instanceof EntitySeat){
							EntitySeat seat = (EntitySeat) child;
							if(seat.isController){
								if(seat.getPassenger() != null){
									attacker = seat.getPassenger();
									break;
								}
							}
						}
						
					}
					for(int i=0; i < collidedEntites.size(); ++i){
						if(!(collidedEntites.get(i).getRidingEntity() instanceof EntitySeat)){
							collidedEntites.get(i).attackEntityFrom(new DamageSourceWheel(attacker), (float) (ConfigSystem.getDoubleConfig("WheelDamageFactor")*mover.velocity*mover.currentMass/1000F));
						}
					}
				}
			}
		}else if(!(parent instanceof EntityCar)){
			if(mover.brakeOn || mover.parkingBrakeOn){
				angularVelocity = 0;
			}else if(angularVelocity>0){
				angularVelocity = (float) Math.max(angularVelocity - 0.05, 0);
			}
		}
		angularPosition += Math.toDegrees(angularVelocity);
		if(worldObj.isRemote){
			MTS.proxy.updateSFXEntity(this, worldObj);
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
		return this.packInfo.groundDevice.thickness;
	}
	
	@Override
	public float getHeight(){
		return this.isFlat ? this.packInfo.groundDevice.flatDiameter : this.packInfo.groundDevice.diameter;
	}
	
	@Override
	public Item getItemForPart(){
		return this.isFlat ? null : super.getItemForPart();
	}
	
	@Override
	public Vec3d getRotation(float partialTicks){
		if(this.packInfo.groundDevice.rotatesOnShaft){
			if(this.offset.xCoord > 0){
				if(this.turnsWithSteer){
					return new Vec3d(this.angularPosition + this.angularVelocity*partialTicks, multipart.getSteerAngle(), 0);
				}else{
					return new Vec3d(this.angularPosition + this.angularVelocity*partialTicks, 0, 0);
				}
			}else{
				if(this.turnsWithSteer){
					return new Vec3d(-(this.angularPosition + this.angularVelocity*partialTicks), 180 + multipart.getSteerAngle(), 0);
				}else{
					return new Vec3d(-(this.angularPosition + this.angularVelocity*partialTicks), 180, 0);
				}
			}
		}else{
			return Vec3d.ZERO;
		}
	}
	
	/**Checks to see if this part is on the ground.
	 * Used to see if this ground device needs to affect physics.
	 */
	public boolean isOnGround(){
		return isPartCollidingWithBlocks(groundDetectionOffset);
	}
	
	/**Makes this part flat.  One-way for obvious reaons.
	 */
	public void setFlat(){
		this.isFlat = true;
	}
}
