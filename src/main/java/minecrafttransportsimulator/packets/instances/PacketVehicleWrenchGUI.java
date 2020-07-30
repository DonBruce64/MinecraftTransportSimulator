package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.BuilderGUI;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.guis.instances.GUIInstruments;
import minecrafttransportsimulator.guis.instances.GUIVehicleEditor;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Packet used to open GUIs when a vehicle is clicked by a wrench.
 * This is sent by the server to the client who clicked the vehicle
 * once it verifies that the player can actually interact with the
 *  vehicle (owns it, is OP, etc.).
 * 
 * @author don_bruce
 */
public class PacketVehicleWrenchGUI extends APacketVehicle{
	
	public PacketVehicleWrenchGUI(EntityVehicleF_Physics vehicle){
		super(vehicle);
	}
	
	public PacketVehicleWrenchGUI(ByteBuf buf){
		super(buf);
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle){
		if(vehicle.equals(player.getEntityRiding()) && ConfigSystem.configObject.client.devMode.value){
			BuilderGUI.openGUI(new GUIVehicleEditor(vehicle));
		}else{
			BuilderGUI.openGUI(new GUIInstruments(vehicle, player));
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
