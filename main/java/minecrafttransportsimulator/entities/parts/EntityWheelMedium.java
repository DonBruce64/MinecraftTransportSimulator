package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class EntityWheelMedium extends EntityWheel{
	public EntityWheelMedium(World world){
		super(world);
	}
	
	public EntityWheelMedium(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ);
	}

	@Override
	public float getWidth(){
		return 0.375F;
	}
	
	@Override
	public float getHeight(){
		return 0.75F;
	}
	
	@Override
	public ItemStack getItemStack(){
		return new ItemStack(MTSRegistry.wheelMedium);
	}
	
	@Override
	public EntityWheel getFlatVersion(){
		return new EntityWheelMediumFlat(this.worldObj, this.parent, this.parentUUID, this.offsetX, this.offsetY, this.offsetZ, this.propertyCode);
	}
	
	public static class EntityWheelMediumFlat extends EntityWheelMedium{
		public EntityWheelMediumFlat(World world){
			super(world);
		}
		
		public EntityWheelMediumFlat(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
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
