package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Packet used to toggle variable states.  Sent from clients to servers to
 * tell them to change the custom state of a vehicle, and then sent back
 * to all clients to have them update those states.
 * 
 * @author don_bruce
 */
public class PacketVehicleVariableToggle extends APacketVehicle{
	private final String variableName;
	
	public PacketVehicleVariableToggle(EntityVehicleF_Physics vehicle, String variableName){
		super(vehicle);
		this.variableName = variableName;
	}
	
	public PacketVehicleVariableToggle(ByteBuf buf){
		super(buf);
		this.variableName = readStringFromBuffer(buf);
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(variableName, buf);
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle){
		if(vehicle.variablesOn.contains(variableName)){
			vehicle.variablesOn.remove(variableName);
		}else{
			vehicle.variablesOn.add(variableName);
		}
		return true;
	}
}
