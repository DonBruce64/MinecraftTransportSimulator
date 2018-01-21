package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityChildInventory;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityVehicleChest extends EntityChildInventory{
	
	public EntityVehicleChest(World world){
		super(world);
	}
	
	public EntityVehicleChest(World world, EntityMultipartParent moving, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityMultipartMoving) moving, parentUUID, offsetX, offsetY, offsetZ, 0.75F, 0.75F);
	}
	
	@Override
	protected boolean attackChild(DamageSource source, float damage){
		return false;
	}

	@Override
	public void setNBTFromStack(ItemStack stack){}

	@Override
	public ItemStack getItemStack(){
		return new ItemStack(Item.getItemFromBlock(Blocks.CHEST));
	}
	
	@Override
	protected String getChildInventoryName(){
		return "entity." + MTS.MODID + ".Chest.name";
	}
	
	protected boolean collidesWithBlocks(){
		return false;
	}
}
