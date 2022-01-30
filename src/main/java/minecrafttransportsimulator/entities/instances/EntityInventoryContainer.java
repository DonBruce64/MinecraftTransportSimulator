package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.baseclasses.IInventoryProvider;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketInventoryContainerChange;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**Basic inventory class.  Class contains methods for adding and removing items from the inventory, as well as automatic
 * syncing of items across clients and servers.  This allows the inventory to be put on any object
 * without the need to worry about packets getting out of whack.
 *
 * @author don_bruce
 */
public class EntityInventoryContainer extends AEntityA_Base implements IInventoryProvider{
	private final NonNullList<ItemStack> inventory;
	
	public EntityInventoryContainer(WrapperWorld world, WrapperNBT data, int maxSlots){
		super(world, data);
		this.inventory = NonNullList.<ItemStack>withSize((maxSlots), ItemStack.EMPTY);
		ItemStackHelper.loadAllItems(data.tag, inventory);
	}
	
	@Override
	public double getMass(){
		return getInventoryMass();
	}

	@Override
	public int getSize(){
		return inventory.size();
	}
	
	@Override
	public ItemStack getStack(int index){
		return inventory.get(index);
	}
	
	@Override
	public void setStack(ItemStack stackToSet, int index){
		inventory.set(index, stackToSet.copy());
		if(!world.isClient()){
			InterfacePacket.sendToAllClients(new PacketInventoryContainerChange(this, index, stackToSet));
		}
	}
	
	/**
	 *  Saves inventory data to the passed-in NBT.
	 */
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		ItemStackHelper.saveAllItems(data.tag, inventory);
		return data;
	}
}
