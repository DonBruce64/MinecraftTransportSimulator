package minecrafttransportsimulator.mcinterface;

import java.util.Map;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemPart;

/**Wrapper for inventories.  This class has multiple constructors to allow for access
 * to multiple inventory containers.  However, some constructors are not visible,
 * as they are for objects that are not assured to have inventories, and as such should
 * not attempt to cast said objects as inventories.
 *
 * @author don_bruce
 */
public interface IWrapperInventory{
	
	/**
	 *  Returns the total number of stacks that can fit into this inventory.
	 */
	public int getSize();
	
	/**
	 *  Returns the stack in the specified slot.
	 */
	public IWrapperItemStack getStackInSlot(int slot);
	
	/**
	 *  Returns the item in the specified slot.  Only valid for {@link AItemBase} items.
	 */
	public AItemBase getItemInSlot(int slot);
	
	/**
	 *  Removes a single item from the passed-in slot.
	 */
	public void decrementSlot(int slot);
	
	/**
	 *  Returns true if this inventory contains the passed-in item.
	 */
	public boolean hasItem(AItemBase itemToFind);
	
	/**
	 *  Adds the passed-in stack to this inventory.  If not possible, false is returned.
	 */
	public boolean addStack(IWrapperItemStack wrapperStack);
	
	/**
	 *  Adds the passed-in item to this inventory.  Does not care about slot position.
	 *  Returns true if addition was successful.
	 */
	public boolean addItem(AItemBase item, IWrapperNBT data);
	
	/**
	 *  Attempts to remove the passed-in number of items matching those in the stack
	 *  from this inventory.  Returns true if all the items were removed, false if
	 *  only some of the items were removed.
	 */
	public boolean removeStack(IWrapperItemStack stack, int qty);
	
	/**
	 *  Attempts to remove the passed-in item from this inventory.
	 *  Returns true if removal was successful.
	 */
	public boolean removeItem(AItemBase item, IWrapperNBT data);

	/**
	 *  Returns true if this inventory has all the materials to make the pack-based item.
	 */
	public boolean hasMaterials(AItemPack<?> item);
	
	/**
	 *  Removes all materials from the inventory required to craft the passed-in item.
	 *  {@link #hasMaterials(AItemPack)} MUST be called before this method to ensure
	 *  the the inventory actually has the required materials.  Failure to do so will
	 *  result in the this method removing the incorrect number of materials.
	 */
	public void removeMaterials(AItemPack<?> item);
	
	/**
	 * Gets the weight of this inventory.
	 */
	public float getInventoryWeight(Map<String, Double> heavyItems);
	
	/**
	 *  Gets the explosive power of this inventory.  Used when this container is blown up.
	 *  For our calculations, only ammo is checked.  While we could check for fuel, we assume
	 *  that fuel-containing items are stable enough to not blow up when this container is hit.
	 */
	public default double getExplosiveness(){
		double explosivePower = 0;
		for(int i=0; i<getSize(); ++i){
			IWrapperItemStack stack = getStackInSlot(i);
			if(stack.getItem() instanceof ItemPart){
				ItemPart part = ((ItemPart) stack.getItem());
				if(part.definition.bullet != null){
					explosivePower += stack.getSize()*part.definition.bullet.diameter/10D;
				}
			}
		}
		return explosivePower;
	}
}