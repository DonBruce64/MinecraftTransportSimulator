package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Packet used to toggle custom variable states.  Sent from clients to servers to
 * tell them to change the custom state of a vehicle, and then sent back
 * to all clients to have them update those states..
 * 
 * @author don_bruce
 */
public class PacketVehicleCustomToggle extends APacketVehicle{
	private final String customName;
	
	public PacketVehicleCustomToggle(EntityVehicleF_Physics vehicle, String customName){
		super(vehicle);
		this.customName = customName;
	}
	
	public PacketVehicleCustomToggle(ByteBuf buf){
		super(buf);
		this.customName = readStringFromBuffer(buf);
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(customName, buf);
	}
	
	@Override
	public boolean handle(IWrapperWorld world, IWrapperPlayer player, EntityVehicleF_Physics vehicle){
		if(vehicle.customsOn.contains(customName)){
			vehicle.customsOn.remove(customName);
		}else{
			vehicle.customsOn.add(customName);
		}
		return true;
	}
}
