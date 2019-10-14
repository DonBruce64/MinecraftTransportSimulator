package minecrafttransportsimulator.packets.parts;

import minecrafttransportsimulator.mcinterface.MTSNetwork;
import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.packets.vehicles.APacketVehiclePart;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.nbt.NBTTagCompound;

/**This packet is sent to servers to notify them of an
 * action a client did to change the state of an engine.
 * After processing this packet will be sent to any
 * clients that are tracking this vehicle to allow
 * their engines to update.
 * 
 * @author don_bruce
 */
public class PacketPartCommonEngineSignal extends APacketVehiclePart{
	private byte packetType;

	public PacketPartCommonEngineSignal(){}
	
	public PacketPartCommonEngineSignal(APartEngine engine, PacketEngineTypes packetType){
		super(engine);
		this.packetType = (byte) packetType.ordinal();
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		packetType = tag.getByte("packetType");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setByte("packetType", packetType);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		APartEngine engine = (APartEngine) getPart(world);
		if(engine != null){
			switch(PacketEngineTypes.values()[packetType]){
				case MAGNETO_OFF: engine.setMagnetoStatus(false); break;
				case MAGNETO_ON: engine.setMagnetoStatus(true); break;
				case ES_OFF: engine.setElectricStarterStatus(false); break;
				case ES_ON: engine.setElectricStarterStatus(true); break;
				case HS_ON: engine.handStartEngine(); break;
				case BACKFIRE: engine.backfireEngine(); break;
				case START: engine.startEngine(); break;
				default: engine.stallEngine(PacketEngineTypes.values()[packetType]); break;
			}
			if(onServer){
				MTSNetwork.sendPacketToPlayersTracking(this, engine.vehicle);
			}
		}
	}
	
	public enum PacketEngineTypes{
		MAGNETO_OFF,
		MAGNETO_ON,
		ES_OFF,
		ES_ON,
		HS_ON,
		BACKFIRE,
		START,
		FUEL_OUT,
		TOO_SLOW,
		DROWN;
	}
}
