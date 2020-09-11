package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.packets.components.APacketVehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartGun;

/**Packet used to send signals to guns.  This can be either to start/stop the firing of the gun,
 * or to re-load the gun with the specified bullets.  If we are doing start/stop commands, then
 * this packet first gets sent to the server from the client who requested the command.  After this,
 * it is send to all players tracking the gun.  If this packet is for re-loading bullets, then it will
 * only appear on clients after the server has verified the bullets can in fact be loaded.
 * 
 * @author don_bruce
 */
public class PacketVehiclePartGun extends APacketVehiclePart{
	private final boolean controlPulse;
	private final boolean triggerState;
	private final String bulletPackID;
	private final String bulletSystemName;
	
	public PacketVehiclePartGun(PartGun gun, boolean triggerState){
		super(gun.vehicle, gun.placementOffset);
		this.controlPulse = true;
		this.triggerState = triggerState;
		this.bulletPackID = null;
		this.bulletSystemName = null;
	}
	
	public PacketVehiclePartGun(PartGun gun, String bulletPackID, String bulletSystemName){
		super(gun.vehicle, gun.placementOffset);
		this.controlPulse = false;
		this.triggerState = false;
		this.bulletPackID = bulletPackID;
		this.bulletSystemName = bulletSystemName;
	}
	
	public PacketVehiclePartGun(ByteBuf buf){
		super(buf);
		this.controlPulse = buf.readBoolean();
		if(controlPulse){
			this.triggerState = buf.readBoolean();
			this.bulletPackID = null;
			this.bulletSystemName = null;
		}else{
			this.triggerState = false;
			this.bulletPackID = readStringFromBuffer(buf);
			this.bulletSystemName = readStringFromBuffer(buf);
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeBoolean(controlPulse);
		if(controlPulse){
			buf.writeBoolean(triggerState);
		}else{
			writeStringToBuffer(bulletPackID, buf);
			writeStringToBuffer(bulletSystemName, buf);
		}
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle, Point3d offset){
		PartGun gun = (PartGun) vehicle.getPartAtLocation(offset);
		if(controlPulse){
			gun.firing = triggerState;
		}else{
			gun.tryToReload((ItemPart) MTSRegistry.packItemMap.get(bulletPackID).get(bulletSystemName));
		}
		return true;
	}
}
