package minecrafttransportsimulator.entities.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.IInventoryProvider;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketInventoryContainerChange;

/**
 * Basic inventory class.  Class contains methods for adding and removing items from the inventory, as well as automatic
 * syncing of items across clients and servers.  This allows the inventory to be put on any object
 * without the need to worry about packets getting out of whack.
 *
 * @author don_bruce
 */
public class EntityInventoryContainer extends AEntityA_Base implements IInventoryProvider {
    private static final IWrapperNBT blankData = InterfaceManager.coreInterface.getNewNBTWrapper();
    private final List<IWrapperItemStack> inventory;
    private final int stackSize;

    public EntityInventoryContainer(AWrapperWorld world, IWrapperNBT data, int maxSlots) {
        this(world, data, maxSlots, 64);
    }

    public EntityInventoryContainer(AWrapperWorld world, IWrapperNBT data, int maxSlots, int stackSize) {
        super(world, data);
        if (data != null) {
            this.inventory = data.getStacks(maxSlots);
        } else {
            //Load from blank data to ensure we populate empty stacks.
            this.inventory = blankData.getStacks(maxSlots);
        }
        this.stackSize = stackSize;
    }

    @Override
    public EntityAutoUpdateTime getUpdateTime() {
        //Furnaces don't need to tick.
        return EntityAutoUpdateTime.NEVER;
    }

    @Override
    public double getMass() {
        return getInventoryMass();
    }

    @Override
    public int getSize() {
        return inventory.size();
    }

    @Override
    public int getStackSize() {
        return stackSize;
    }

    @Override
    public IWrapperItemStack getStack(int index) {
        return inventory.get(index);
    }

    @Override
    public void setStack(IWrapperItemStack stackToSet, int index) {
        inventory.set(index, stackToSet);
        if (!world.isClient()) {
            InterfaceManager.packetInterface.sendToAllClients(new PacketInventoryContainerChange(this, index, stackToSet));
        }
    }

    /**
     * Saves inventory data to the passed-in NBT.
     */
    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setStacks(inventory);
        return data;
    }
}
