package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import net.minecraft.item.ItemStack;

/**Chest tile entity.
 *
 * @author don_bruce
 */
public class TileEntityChest extends TileEntityDecor{
	
	public final EntityInventoryContainer inventory;
	
	public TileEntityChest(WrapperWorld world, Point3d position, WrapperNBT data){
		super(world, position, data);
		this.inventory = new EntityInventoryContainer(world, data.getDataOrNew("inventory"), (int) (definition.decor.inventoryUnits*9F));
	}

	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("inventory_count"): {
				if(inventory != null){
					return inventory.getCount();
				}else{
					return 0;
				}
			}
			case("inventory_percent"): {
				if(inventory != null){
					return inventory.getCount()/(double)inventory.getSize();
				}else{
					return 0;
				}
			}
			case("inventory_capacity"): {
				if(inventory != null){
					return inventory.getSize();
				}else{
					return 0;
				}
			}
		}
		
		return super.getRawVariableValue(variable, partialTicks);
	}
	
	@Override
	public void addDropsToList(List<ItemStack> drops){
		WrapperNBT data = new WrapperNBT();
		save(data);
		ItemStack droppedStack = getItem().getNewStack();
		droppedStack.setTagCompound(data.tag);
		drops.add(droppedStack);
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		if(inventory != null){
			data.setData("inventory", inventory.save(new WrapperNBT()));
		}
		return data;
	}
}
