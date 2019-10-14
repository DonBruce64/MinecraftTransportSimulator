package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;

/**This packet is sent to clients when a vehicle has
 * fixed a window.  Decreases broken windows by 1, causing
 * a window to re-appear on the model.
 * 
 * @author don_bruce
 */
public class PacketVehicleClientWindowFix extends APacketVehicle{

	public PacketVehicleClientWindowFix(){}
	
	public PacketVehicleClientWindowFix(EntityVehicleB_Existing vehicle){
		super(vehicle);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		--((EntityVehicleB_Existing) getVehicle(world)).brokenWindows;
	}
}
