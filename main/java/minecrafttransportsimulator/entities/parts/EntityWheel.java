package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityGroundDevice;
import minecrafttransportsimulator.packets.general.FlatWheelPacket;
import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.systems.SFXSystem.SFXEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class EntityWheel extends EntityGroundDevice implements SFXEntity{
	public boolean isFlat;
	public float angularPosition;
	public float angularVelocity;
	protected float wheelDiameter;
	private boolean landedThisTick = false;
	private EntityMultipartMoving moving;
	
	public EntityWheel(World world){
		super(world);
	}
	
	public EntityWheel(World world, EntityMultipartMoving moving, String parentUUID, float offsetX, float offsetY, float offsetZ, float width, float height){
		super(world, moving, parentUUID, offsetX, offsetY, offsetZ, width, height, 0.5F, 0.5F);
	}
	
	@Override
	public void setNBTFromStack(ItemStack stack){}
	
	@Override
	protected boolean attackChild(DamageSource source, float damage){
		if(!isFlat){
			if(source.isExplosion() || Math.random() < 0.1){
				setFlat();
			}
		}
		return true;
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		moving = (EntityMultipartMoving) this.parent;
		if(this.isOnGround()){
			if(angularVelocity/(moving.velocity/wheelDiameter) < 0.25 && moving.velocity > 0.1){
				if(worldObj.getBlockState(this.getPosition().down()).getBlockHardness(worldObj, this.getPosition().down()) >= 1.5){
					landedThisTick = true;
				}
			}
			angularVelocity = (float) (moving.velocity/wheelDiameter);
		}else{
			if(moving.brakeOn || moving.parkingBrakeOn){
				angularVelocity = 0;
			}else if(angularVelocity>0){
				angularVelocity = (float) Math.max(angularVelocity - 0.05, 0);
			}
		}
		angularPosition += angularVelocity;
		
		if(worldObj.isRemote){
			MTS.proxy.updateSFXEntity(this, worldObj);
		}
	}
	
	public void setFlat(){
		isFlat = true;
		this.offsetY+=this.height/4;
		this.height*=0.5;
		this.motiveFriction*=10;
		this.lateralFriction*=0.1;
		if(!worldObj.isRemote){
			worldObj.newExplosion(this, posX, posY, posZ, 0.25F, false, false);
			MTS.MTSNet.sendToAll(new FlatWheelPacket(this.getEntityId()));
		}
	}
	
	@SideOnly(Side.CLIENT)
	public boolean hasSound(){
		return false;
	}
	
	@SideOnly(Side.CLIENT)
	public MovingSound getNewSound(){
		return null;
	}
	
	@SideOnly(Side.CLIENT)
	public MovingSound getCurrentSound(){
		return null;
	}
	
	@SideOnly(Side.CLIENT)
	public void setCurrentSound(MovingSound sound){
		
	}
	
	@SideOnly(Side.CLIENT)
	public boolean shouldSoundBePlaying(){
		return false;
	}
	
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
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.isFlat=tagCompound.getBoolean("isFlat");
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("isFlat", this.isFlat);
		return tagCompound;
	}
	
	public static class EntityWheelSmall extends EntityWheel{
		public EntityWheelSmall(World world){
			super(world);
			this.wheelDiameter=0.4375F;
		}
		
		public EntityWheelSmall(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
			super(world, (EntityMultipartMoving) parent, parentUUID, offsetX, offsetY, offsetZ, 0.5F, 0.5F);
		}

		@Override
		public ItemStack getItemStack(){
			if(!this.isFlat){
				return new ItemStack(MTSRegistry.wheelSmall);	
			}else{
				return null;
			}
		}
	}
	
	public static class EntityWheelLarge extends EntityWheel{
		public EntityWheelLarge(World world){
			super(world);
			this.wheelDiameter=0.6875F;
		}
		
		public EntityWheelLarge(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
			super(world, (EntityMultipartMoving) parent, parentUUID, offsetX, offsetY, offsetZ, 0.75F, 0.75F);
		}

		@Override
		public ItemStack getItemStack(){
			if(!this.isFlat){
				return new ItemStack(MTSRegistry.wheelLarge);	
			}else{
				return null;
			}
		}
	}
}
