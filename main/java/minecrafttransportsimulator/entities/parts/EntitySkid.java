package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.multipart.parts.PartGroundDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class EntitySkid extends PartGroundDevice{
	public EntitySkid(World world){
		super(world);
	}
	
	public EntitySkid(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ);
	}
	
	@Override
	public float getWidth(){
		return 0.3F;
	}

	@Override
	public float getHeight(){
		return 0.3F;
	}
	
	@Override
	public boolean shouldAffectSteering(){
		return true;
	}

	@Override
	public void setNBTFromStack(ItemStack stack){}

	@Override
	public ItemStack getItemStack(){
		return new ItemStack(MTSRegistry.skid);
	}
	
	@Override
	public float getMotiveFriction(){
		return 0.0F;
	}
	
	@Override
	public float getLateralFriction(){
		return 0.5F;
	}
}
