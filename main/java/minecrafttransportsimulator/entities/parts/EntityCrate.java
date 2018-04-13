package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityChildInventory;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class EntityCrate extends EntityChildInventory{
	
	public EntityCrate(World world){
		super(world);
	}
	
	public EntityCrate(World world, EntityMultipartParent moving, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityMultipartMoving) moving, parentUUID, offsetX, offsetY, offsetZ);
	}
	
	@Override
	public float getWidth(){
		return 0.75F;
	}

	@Override
	public float getHeight(){
		return 0.75F;
	}

	@Override
	public void setNBTFromStack(ItemStack stack){}

	@Override
	public ItemStack getItemStack(){
		return new ItemStack(MTSRegistry.crate);
	}
	
	@Override
	protected String getChildInventoryName(){
		return "entity." + MTS.MODID + ".crate.name";
	}
}
