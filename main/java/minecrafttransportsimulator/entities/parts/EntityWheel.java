package minecrafttransportsimulator.entities.parts;

import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSDamageSources.DamageSourceWheel;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityCar;
import minecrafttransportsimulator.multipart.parts.PartGroundDevice;
import minecrafttransportsimulator.packets.parts.PacketFlatGroundDevice;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.systems.SFXSystem.SFXEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class EntityWheel extends PartGroundDevice implements SFXEntity{
	public float angularPosition;
	public float angularVelocity;
	private boolean landedThisTick = false;
	private EntityMultipartMoving mover;
	
	public EntityWheel(World world){
		super(world);
	}
	
	public EntityWheel(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ);
	}
	
	@Override
	public void setNBTFromStack(ItemStack stack){}
	
	@Override
	public String getTextureName(){
		return "wheel";
	}
	
	@Override
	public float getXRotation(float partialTicks){
		return angularPosition + angularVelocity*partialTicks;
	}
	
	@Override
	public float getMotiveFriction(){
		return !this.isFlat() ? this.getWidth() : this.getWidth()/10F;
	}
	
	@Override
	public float getLateralFriction(){
		return !this.isFlat() ? this.getWidth() : this.getWidth()/10F;
	}
	
	@Override
	public void attackPart(DamageSource source, float damage){
		if(!this.isFlat()){
			if(source.isExplosion() || Math.random() < 0.1){
				if(!worldObj.isRemote){
					worldObj.newExplosion(this, posX, posY, posZ, 0.25F, false, false);
					//Replace regular wheel with flat wheel.
					EntityWheel flatWheel = this.getFlatVersion();
					parent.removeChild(this.UUID, false);
					parent.addChild(flatWheel.UUID, flatWheel, true);
					MTS.MTSNet.sendToAll(new PacketFlatGroundDevice(this.getEntityId()));
				}
			}
		}
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		mover = (EntityMultipartMoving) this.parent;
		if(this.isOnGround()){
			if(angularVelocity/(mover.velocity/getHeight()) < 0.25 && mover.velocity > 0.3){
				if(worldObj.getBlockState(this.getPosition().down()).getBlockHardness(worldObj, this.getPosition().down()) >= 1.5){
					landedThisTick = true;
				}
			}
			angularVelocity = (float) (mover.velocity/getHeight());
			
			if(!worldObj.isRemote && mover.velocity > 0.2F){
				List<EntityLivingBase> collidedEntites = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().expand(0.25F, 0, 0.25F));
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
	
	public boolean isFlat(){
		return false;
	}
	
	public EntityWheel getFlatVersion(){
		return null;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public MovingSound getNewSound(){
		return null;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public MovingSound getCurrentSound(){
		return null;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void setCurrentSound(MovingSound sound){}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSoundBePlaying(){
		return false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getVolume(){
		return 0.0F;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getPitch(){
		return 0.0F;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void spawnParticles(){
		if(landedThisTick){
			for(byte i=0; i<4; ++i){
				Minecraft.getMinecraft().effectRenderer.addEffect(new SFXSystem.WhiteSmokeFX(worldObj, posX, posY, posZ, Math.random()*0.10 - 0.05, 0.15, Math.random()*0.10 - 0.05));
			}
			MTS.proxy.playSound(this, MTS.MODID + ":" + "wheel_striking", 1, 1);
			landedThisTick = false;
		}
	}
}
