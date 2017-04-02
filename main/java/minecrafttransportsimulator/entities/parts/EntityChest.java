package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityChildInventory;
import minecrafttransportsimulator.entities.core.EntityParent;
import minecrafttransportsimulator.entities.core.EntityVehicle;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class EntityChest extends EntityChildInventory{
	
	public EntityChest(World world){
		super(world);
	}
	
	public EntityChest(World world, EntityParent vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityVehicle) vehicle, parentUUID, offsetX, offsetY, offsetZ, 0.75F, 0.75F);
	}

	@Override
	public void setNBTFromStack(ItemStack stack){}

	@Override
	public ItemStack getItemStack(){
		return new ItemStack(Item.getItemFromBlock(Blocks.chest));
	}
	
	@Override
	protected String getChildInventoryName(){
		return "entity." + MTS.MODID + ".Chest.name";
	}
}
