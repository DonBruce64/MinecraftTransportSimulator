package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packets.components.APacketVehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;

/**Packet used to send signals to propellers.  Currently only used to play a breaking sound
 * when propeller are removed from the vehicle due to high damage.
 * 
 * @author don_bruce
 */
public class PacketVehiclePartPropeller extends APacketVehiclePart{
	
	public PacketVehiclePartPropeller(PartPropeller propeller){
		super(propeller.vehicle, propeller.placementOffset);
	}
	
	public PacketVehiclePartPropeller(ByteBuf buf){
		super(buf);
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle, Point3d offset){
		//FIXME play different sound for removing things.  Preferabley through the wrapper.
		//this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 2.0F, 1.0F);
		return true;
	}
}
