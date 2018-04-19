package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class EntityWheelLarge extends EntityWheel{
	public EntityWheelLarge(World world){
		super(world);
	}
	
	public EntityWheelLarge(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ);
	}

	@Override
	public float getWidth(){
		return 0.5F;
	}
	
	@Override
	public float getHeight(){
		return 1.0F;
	}
	
	@Override
	public ItemStack getItemStack(){
		return new ItemStack(MTSRegistry.wheelLarge);
	}
	
	@Override
	public EntityWheel getFlatVersion(){
		return new EntityWheelLargeFlat(this.worldObj, this.parent, this.parentUUID, this.offsetX, this.offsetY, this.offsetZ, this.propertyCode);
	}
	
	public static class EntityWheelLargeFlat extends EntityWheelLarge{
		public EntityWheelLargeFlat(World world){
			super(world);
		}
		
		public EntityWheelLargeFlat(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
			super(world, parent, parentUUID, offsetX, offsetY, offsetZ, propertyCode);
		}
		
		@Override
		public float getHeight(){
			return super.getHeight()/2F;
		}
		
		@Override
		public ItemStack getItemStack(){
			return null;
		}
		
		@Override
		public boolean isFlat(){
			return true;
		}
	}
}
