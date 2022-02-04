package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet sent to inventory containers to update the inventory in them.
 * 
 * @author don_bruce
 */
public class PacketInventoryContainerChange extends APacketEntity<EntityInventoryContainer>{
	private final int index;
	private final WrapperItemStack stackToChangeTo;
	
	public PacketInventoryContainerChange(EntityInventoryContainer inventory, int index, WrapperItemStack stackToChangeTo){
		super(inventory);
		this.index = index;
		this.stackToChangeTo = stackToChangeTo;
	}
	
	public PacketInventoryContainerChange(ByteBuf buf){
		super(buf);
		this.index = buf.readInt();
		this.stackToChangeTo = readDataFromBuffer(buf).getStacks(1).get(0);
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(index);
		WrapperNBT stackData = new WrapperNBT();
		List<WrapperItemStack> stackList = new ArrayList<WrapperItemStack>();
		stackList.add(stackToChangeTo);
		stackData.setStacks(stackList);
		writeDataToBuffer(stackData, buf);
	}
	
	@Override
	public boolean handle(WrapperWorld world, EntityInventoryContainer inventory){
		inventory.setStack(stackToChangeTo, index);
		return true;
	}
}
