package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.baseclasses.IInventoryProvider;
import net.minecraft.inventory.IInventory;

/**Wrapper for inventories.  This is mainly for the player, but works for any inventory in the game.
 * Say for inventories of MC crates.
 *
 * @author don_bruce
 */
public class WrapperInventory implements IInventoryProvider{
	private final IInventory inventory;
	
	public WrapperInventory(IInventory inventory){
		this.inventory = inventory;
	}
	
	@Override
	public int getSize(){
		return inventory.getSizeInventory();
	}
	
	@Override
	public WrapperItemStack getStack(int slot){
		return new WrapperItemStack(inventory.getStackInSlot(slot));
	}
	
	@Override
	public void setStack(WrapperItemStack stackToSet, int index){
		inventory.setInventorySlotContents(index, stackToSet.stack);
		inventory.markDirty();
	}
}