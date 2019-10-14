package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;

/**This packet is sent to clients when a vehicle has
 * broken a window.  Increases broken windows by 1, causing
 * one less window to render on the model.
 * 
 * @author don_bruce
 */
public class PacketVehicleClientWindowBreak extends APacketVehicle{

	public PacketVehicleClientWindowBreak(){}
	
	public PacketVehicleClientWindowBreak(EntityVehicleB_Existing vehicle){
		super(vehicle);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		++((EntityVehicleB_Existing) getVehicle(world)).brokenWindows;
	}
}
