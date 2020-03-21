package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.guis.instances.GUIInstruments;
import minecrafttransportsimulator.guis.instances.GUIVehicleEditor;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

/**Packet used to open GUIs when a vehicle is clicked by a wrench.
 * This is sent by the server to the client who clicked the vehicle
 * once it verifies that the player can actually interact with the
 *  vehicle (owns it, is OP, etc.).
 * 
 * @author don_bruce
 */
public class PacketVehicleWrenchGUI extends APacketVehicle{
	
	public PacketVehicleWrenchGUI(EntityVehicleE_Powered vehicle){
		super(vehicle);
	}
	
	public PacketVehicleWrenchGUI(ByteBuf buf){
		super(buf);
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleE_Powered vehicle){
		if(player.isRidingVehicle(vehicle) && ConfigSystem.configObject.client.devMode.value){
			WrapperGUI.openGUI(new GUIVehicleEditor(vehicle));
		}else{
			WrapperGUI.openGUI(new GUIInstruments(vehicle, player));
		}
		return true;
	}
	
	public enum Controls{
		THROTTLE,
		STEERING,
		AILERON,
		ELEVATOR,
		RUDDER;
	}
}
