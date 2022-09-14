package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to inventory containers to update the inventory in them.
 *
 * @author don_bruce
 */
public class PacketInventoryContainerChange extends APacketEntity<EntityInventoryContainer> {
    private final int index;
    private final IWrapperItemStack stackToChangeTo;

    public PacketInventoryContainerChange(EntityInventoryContainer inventory, int index, IWrapperItemStack stackToChangeTo) {
        super(inventory);
        this.index = index;
        this.stackToChangeTo = stackToChangeTo;
    }

    public PacketInventoryContainerChange(ByteBuf buf) {
        super(buf);
        this.index = buf.readInt();
        this.stackToChangeTo = readDataFromBuffer(buf).getStacks(1).get(0);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(index);
        IWrapperNBT stackData = InterfaceManager.coreInterface.getNewNBTWrapper();
        List<IWrapperItemStack> stackList = new ArrayList<>();
        stackList.add(stackToChangeTo);
        stackData.setStacks(stackList);
        writeDataToBuffer(stackData, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityInventoryContainer inventory) {
        inventory.setStack(stackToChangeTo, index);
        return true;
    }
}
