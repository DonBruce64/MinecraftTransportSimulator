package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.jsondefs.JSONDecor.FurnaceComponentType;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import net.minecraft.item.ItemStack;

/**Basic furnace class.  Class is essentially an inventory that holds state of smelting
 * operations.  Has a method for ticking
 *
 * @author don_bruce
 */
public class EntityFurnace extends EntityInventoryContainer{
	private int ticksToSmelt;
	private int ticksLeftOfFuel;
	private int ticksLeftToSmelt;
	public final FurnaceComponentType type;
	
	public static final int SMELTING_ITEM_SLOT = 0;
	public static final int SMELTED_ITEM_SLOT = 1;
	public static final int FUEL_ITEM_SLOT = 2;
	
	public EntityFurnace(WrapperWorld world, WrapperNBT data, FurnaceComponentType type){
		super(world, data, 3);
		this.ticksLeftOfFuel = data.getInteger("ticksLeftOfFuel");
		this.ticksLeftToSmelt = data.getInteger("ticksLeftToSmelt");
		this.type = type;
	}
	
	@Override
	public int addStack(ItemStack stackToAdd, boolean doAdd){
		//Only allow smeltable and fuel, and only in appropriate stacks.
		if(!InterfaceCore.getSmeltedItem(stackToAdd).isEmpty() || (type.equals(FurnaceComponentType.FUEL) && InterfaceCore.getFuelValue(stackToAdd) != 0)){
			return super.addStack(stackToAdd, doAdd);
		}else{
			return 0;
		}
	}
	
	/**
	 *  Saves tank data to the passed-in NBT.
	 */
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setInteger("ticksLeftOfFuel", ticksLeftOfFuel);
		data.setInteger("ticksLeftToSmelt", ticksLeftToSmelt);
		return data;
	}
}
