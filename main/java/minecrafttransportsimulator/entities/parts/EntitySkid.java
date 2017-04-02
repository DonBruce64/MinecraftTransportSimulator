package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityGroundDevice;
import minecrafttransportsimulator.entities.core.EntityParent;
import minecrafttransportsimulator.entities.core.EntityVehicle;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class EntitySkid extends EntityGroundDevice{
	public EntitySkid(World world){
		super(world);
		this.setSize(0.3F, 0.3F);
	}
	
	public EntitySkid(World world, EntityParent plane, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityVehicle) plane, parentUUID, offsetX, offsetY, offsetZ, 0.3F, 0.3F, 0.1F, 0.5F);
	}

	@Override
	public void setNBTFromStack(ItemStack stack){}

	@Override
	public ItemStack getItemStack(){
		return new ItemStack(MTSRegistry.skid);
	}
}
