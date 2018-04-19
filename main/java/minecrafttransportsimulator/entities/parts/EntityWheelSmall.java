package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class EntityWheelSmall extends EntityWheel{
	public EntityWheelSmall(World world){
		super(world);
	}
	
	public EntityWheelSmall(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ);
	}

	@Override
	public float getWidth(){
		return 0.25F;
	}
	
	@Override
	public float getHeight(){
		return 0.5F;
	}
	
	@Override
	public ItemStack getItemStack(){
		return new ItemStack(MTSRegistry.wheelSmall);
	}
	
	@Override
	public EntityWheel getFlatVersion(){
		return new EntityWheelSmallFlat(this.worldObj, this.parent, this.parentUUID, this.offsetX, this.offsetY, this.offsetZ, this.propertyCode);
	}
	
	public static class EntityWheelSmallFlat extends EntityWheelSmall{
		public EntityWheelSmallFlat(World world){
			super(world);
		}
		
		public EntityWheelSmallFlat(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
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
