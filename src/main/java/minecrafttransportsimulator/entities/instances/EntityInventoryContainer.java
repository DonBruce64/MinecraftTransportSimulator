package minecrafttransportsimulator.entities.instances;

import java.util.Map;

import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.mcinterface.BuilderItem;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketInventoryContainerChange;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**Basic inventory class.  Class contains methods for adding and removing items from the inventory, as well as automatic
 * syncing of items across clients and servers.  This allows the inventory to be put on any object
 * without the need to worry about packets getting out of whack.
 *
 * @author don_bruce
 */
public class EntityInventoryContainer extends AEntityA_Base{
	private final NonNullList<ItemStack> inventory;
	
	public EntityInventoryContainer(WrapperWorld world, WrapperNBT data, int maxSlots){
		super(world, data);
		this.inventory = NonNullList.<ItemStack>withSize((maxSlots), ItemStack.EMPTY);
		ItemStackHelper.loadAllItems(data.tag, inventory);
	}
	
	@Override
	public double getMass(){
		Map<String, Double> heavyItems = ConfigSystem.configObject.general.itemWeights.weights;
		double currentMass = 0;
		for(ItemStack stack : inventory){
			if(stack != null){
				double weightMultiplier = 1.0;
				for(String heavyItemName : heavyItems.keySet()){
					if(stack.getItem().getRegistryName().toString().contains(heavyItemName)){
						weightMultiplier = heavyItems.get(heavyItemName);
						break;
					}
				}
				currentMass += 5F*stack.getCount()/stack.getMaxStackSize()*weightMultiplier;
			}
		}
		return currentMass;
	}
	
	/**
	 *  Gets the max number of item stacks this inventory can contain.
	 */
	public int getSize(){
		return inventory.size();
	}
	
	/**
	 *  Gets the number of items currently in this container. 
	 */
	public int getCount(){
		int count = 0;
		for(ItemStack stack : inventory){
			if(!stack.isEmpty()){
				++count;
			}
		}
		return count;
	}
	
	/**
	 *  Returns true if this stack can be added to the specified slot.  Normally true for all stacks,
	 *  but can be used to limit what goes where.
	 */
	public boolean isStackValid(ItemStack stackToCheck, int index){
		return true;
	}
	
	/**
	 *  Returns the stack in the specified slot.  This may be used to view the items in this inventory.
	 *  Modifications should not happen directly to the items.  Instead, use:
	 *  {@link #addStack(ItemStack, boolean)} or {@link #removeStack(ItemStack, int, boolean)}.
	 */
	public ItemStack getStack(int index){
		return inventory.get(index);
	}
	
	/**
	 *  Sets the stack in the inventory, overwriting anything that was previously in this slot.
	 *  Mainly used for packet operations, as it can result in the destruction of items.
	 */
	public void setStack(ItemStack stackToSet, int index){
		inventory.set(index, stackToSet.copy());
		if(!world.isClient()){
			InterfacePacket.sendToAllClients(new PacketInventoryContainerChange(this, index, stackToSet));
		}
	}
	
	/**
	 *  Tries to add the passed-in stack to this inventory.  Adds as many items from the
	 *  stack as possible, but may or may not add all of them.  As such, the actual number of items
	 *  added is returned.  If doAdd is false, then no actual transfer into this inventory will occur.
	 */
	public int addStack(ItemStack stackToAdd, boolean doAdd){
		int amountAdded = 0;
		for(int i=0; i<inventory.size(); ++i){
			if(isStackValid(stackToAdd, i)){
				ItemStack stack = inventory.get(i);
				if(stack.isEmpty()){
					if(doAdd){
						inventory.set(i, stackToAdd.copy());
						stack = inventory.get(i);
						stack.setCount(stackToAdd.getCount() - amountAdded);
						if(!world.isClient()){
							InterfacePacket.sendToAllClients(new PacketInventoryContainerChange(this, i, stack));
						}
					}
					return stackToAdd.getCount();
				}else if(stackToAdd.isItemEqual(stack) && (stackToAdd.hasTagCompound() ? stackToAdd.getTagCompound().equals(stack.getTagCompound()) : !stack.hasTagCompound())){
					int amountToAdd = Math.min(stack.getMaxStackSize() - stack.getCount(), stackToAdd.getCount() - amountAdded);
					if(amountToAdd > 0){
						if(doAdd){
							amountAdded += amountToAdd;
							stack.grow(amountToAdd);
							if(!world.isClient()){
								InterfacePacket.sendToAllClients(new PacketInventoryContainerChange(this, i, stack));
							}
						}
					}
					if(amountAdded == stackToAdd.getCount()){
						return amountAdded;
					}
				}
			}
		}
		return amountAdded;
	}
	
	/**
	 *  Adds 1 item to the stack in the passed-in slot.  Returns true if the stack was incremented.
	 */
	public boolean incrementStack(int index){
		ItemStack existingStack = getStack(index);
		if(existingStack.getCount() < existingStack.getMaxStackSize()){
			existingStack.setCount(existingStack.getCount() + 1);
			inventory.set(index, existingStack);
			if(!world.isClient()){
				InterfacePacket.sendToAllClients(new PacketInventoryContainerChange(this, index, existingStack));
			}
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 *  Removes 1 item from the stack in the passed-in slot.  Returns true if the removal is possible, false
	 *  if not (because the stack was empty.
	 */
	public boolean decrementStack(int index){
		ItemStack existingStack = getStack(index);
		if(!existingStack.isEmpty()){
			existingStack.setCount(existingStack.getCount() - 1);
			inventory.set(index, existingStack);
			if(!world.isClient()){
				InterfacePacket.sendToAllClients(new PacketInventoryContainerChange(this, index, existingStack));
			}
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 *  Tries to remove the specified number of items from the passed-in index from this inventory.
	 *  If only some items were able to be removed, then only that number is returned.  doRemove, if 
	 *  set will cause actual removal.  Otherwise the removal is simulated.
	 */
	public int removeItems(int index, int qty, boolean doRemove){
		ItemStack stack = inventory.get(index);
		int stackFinalQty = stack.getCount() > qty ? stack.getCount() - qty : 0;
		if(doRemove){
			stack.setCount(stackFinalQty);
			if(!world.isClient()){
				InterfacePacket.sendToAllClients(new PacketInventoryContainerChange(this, index, stack));
			}
		}
		return stack.getCount() - qty;
		
	}
	
	/**
	 *  Gets the explosive power of this inventory.  Used when this inventory is blown up.
	 */
	public double getExplosiveness(){
		double explosivePower = 0;
		for(ItemStack stack : inventory){
			Item item = stack.getItem();
			if(item instanceof BuilderItem && ((BuilderItem) item).item instanceof ItemBullet){
				ItemBullet bullet = (ItemBullet) ((BuilderItem) item).item;
				if(bullet.definition.bullet != null){
					double blastSize = bullet.definition.bullet.blastStrength == 0 ? bullet.definition.bullet.diameter/10D : bullet.definition.bullet.blastStrength;
					explosivePower += stack.getCount()*blastSize/10D;
				}
			}
		}
		return explosivePower;
	}
	
	/**
	 *  Saves tank data to the passed-in NBT.
	 */
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		ItemStackHelper.saveAllItems(data.tag, inventory);
		return data;
	}
}
