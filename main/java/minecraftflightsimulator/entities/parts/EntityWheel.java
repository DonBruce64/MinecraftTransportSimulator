package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityGroundDevice;
import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.entities.core.EntityVehicle;
import minecraftflightsimulator.registry.MTSRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public abstract class EntityWheel extends EntityGroundDevice{
	public boolean isFlat;
	public float angularPosition;
	public float angularVelocity;
	protected float wheelDiameter;
	private EntityVehicle vehicle;
	
	public EntityWheel(World world){
		super(world);
	}
	
	public EntityWheel(World world, EntityVehicle vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ, float width, float height){
		super(world, vehicle, parentUUID, offsetX, offsetY, offsetZ, width, height, 0.01F, 0.5F);
	}
	
	@Override
	public void setNBTFromStack(ItemStack stack){}
	
	@Override
	public boolean performAttackAction(DamageSource source, float damage){
		if(!worldObj.isRemote){
			if(isDamageWrench(source)){
				return true;
			}
			if(!isFlat){
				if(source.isExplosion() || Math.random() < 0.1){
					setFlat();
				}
			}
		}
		return true;
    }
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		vehicle = (EntityVehicle) this.parent;
		if(worldObj.isRemote){
			if(this.isOnGround()){
				angularVelocity = (float) (vehicle.velocity/wheelDiameter);
			}else{
				if(vehicle.brakeOn || vehicle.parkingBrakeOn){
					angularVelocity = 0;
				}else if(angularVelocity>0){
					angularVelocity = (float) Math.max(angularVelocity - 0.05, 0);
				}
			}
			angularPosition += angularVelocity;
		}
	}
	
	private void setFlat(){
		isFlat = true;
		this.offsetY+=this.height/4;
		this.height*=0.5;
		this.lateralFriction*=0.1;
		this.motiveFriction*=10;
		worldObj.newExplosion(this, posX, posY, posZ, 0.25F, false, false);
		this.sendDataToClient();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.isFlat=tagCompound.getBoolean("isFlat");
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("isFlat", this.isFlat);
	}
	
	public static class EntityWheelSmall extends EntityWheel{
		public EntityWheelSmall(World world){
			super(world);
			this.wheelDiameter=0.4375F;
		}
		
		public EntityWheelSmall(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
			super(world, (EntityVehicle) parent, parentUUID, offsetX, offsetY, offsetZ, 0.5F, 0.5F);
		}

		@Override
		public ItemStack getItemStack(){
			return new ItemStack(MTSRegistry.wheelSmall);
		}
	}
	
	public static class EntityWheelLarge extends EntityWheel{
		public EntityWheelLarge(World world){
			super(world);
			this.wheelDiameter=0.6875F;
		}
		
		public EntityWheelLarge(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
			super(world, (EntityVehicle) parent, parentUUID, offsetX, offsetY, offsetZ, 0.75F, 0.75F);
		}

		@Override
		public ItemStack getItemStack(){
			return new ItemStack(MTSRegistry.wheelLarge);
		}
	}
}
