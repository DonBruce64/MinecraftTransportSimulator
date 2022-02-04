package minecrafttransportsimulator.entities.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.IInventoryProvider;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketInventoryContainerChange;

/**Basic inventory class.  Class contains methods for adding and removing items from the inventory, as well as automatic
 * syncing of items across clients and servers.  This allows the inventory to be put on any object
 * without the need to worry about packets getting out of whack.
 *
 * @author don_bruce
 */
public class EntityInventoryContainer extends AEntityA_Base implements IInventoryProvider{
	private final List<WrapperItemStack> inventory;
	
	public EntityInventoryContainer(WrapperWorld world, WrapperNBT data, int maxSlots){
		super(world, data);
		this.inventory = data.getStacks(maxSlots);
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
	public WrapperItemStack getStack(int index){
		return inventory.get(index);
	}
	
	@Override
	public void setStack(WrapperItemStack stackToSet, int index){
		inventory.set(index, stackToSet);
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
		data.setStacks(inventory);
		return data;
	}
}
