package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;

/**Packet used to send signals to ground devices.  Currently only used to make wheel-typed
 * ground devices flat on clients after they have been set as such on the server.
 * 
 * @author don_bruce
 */
public class PacketPartGroundDevice extends APacketEntity<PartGroundDevice>{
	final boolean flat;
	
	public PacketPartGroundDevice(PartGroundDevice groundDevice, boolean flat){
		super(groundDevice);
		this.flat = flat;
	}
	
	public PacketPartGroundDevice(ByteBuf buf){
		super(buf);
		this.flat = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeBoolean(flat);
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, PartGroundDevice groundDevice){
		groundDevice.setFlatState(flat);
		return true;
	}
}
