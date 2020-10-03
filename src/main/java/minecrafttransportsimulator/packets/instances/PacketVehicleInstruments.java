package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.guis.instances.GUIInstruments;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.items.instances.ItemWrench;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Packet used to change instruments on vehicles.  Sent to the server
 * to process the instrument change, and then sent to all clients if
 * the change is able to be made.  Does not check ownership as that's
 * done before {@link GUIInstruments} is opened by checking for
 * ownership in {@link ItemWrench#doVehicleInteraction}.
 * 
 * @author don_bruce
 */
public class PacketVehicleInstruments extends APacketVehicle{
	private final byte slot;
	private final String instrumentPackID;
	private final String instrumentSystemName;
	
	public PacketVehicleInstruments(EntityVehicleF_Physics vehicle, byte slot, ItemInstrument instrument){
		super(vehicle);
		this.slot = slot;
		if(instrument != null){
			this.instrumentPackID = instrument.definition.packID;
			this.instrumentSystemName = instrument.definition.systemName;
		}else{
			this.instrumentPackID = "";
			this.instrumentSystemName = "";
		}
	}
	
	public PacketVehicleInstruments(ByteBuf buf){
		super(buf);
		this.slot = buf.readByte();
		this.instrumentPackID = readStringFromBuffer(buf);
		this.instrumentSystemName = readStringFromBuffer(buf);
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(slot);
		writeStringToBuffer(instrumentPackID, buf);
		writeStringToBuffer(instrumentSystemName, buf);
	}
	
	@Override
	public boolean handle(IWrapperWorld world, IWrapperPlayer player, EntityVehicleF_Physics vehicle){
		//Check to make sure the instrument can fit in survival player's inventories.
		//Only check this on the server, as adding things to the client doesn't do us any good.
		if(!world.isClient() && !player.isCreative() && vehicle.instruments.containsKey(slot)){
			JSONInstrument definition = vehicle.instruments.get(slot);
			ItemInstrument instrument = (ItemInstrument) PackParserSystem.getItem(definition);
			if(!player.isCreative() && !player.getInventory().addItem(instrument, null)){
				return false;
			}
		}
		
		//If we are removing the instrument, do so now.
		//Otherwise add the instrument.
		if(instrumentPackID.isEmpty()){
			vehicle.instruments.remove(slot);
		}else{
			//Check to make sure player has the instrument they are trying to put in.
			//This is only done on the server, as checking on the client won't make any difference.
			ItemInstrument instrument = PackParserSystem.getItem(instrumentPackID, instrumentSystemName);
			if(!world.isClient() && !player.isCreative()){
				if(player.isCreative() || player.getInventory().hasItem(instrument)){
					player.getInventory().removeItem(instrument, null);
				}else{
					return false;
				}
			}
			vehicle.instruments.put(slot, instrument.definition);
		}
		return true;
	}
}
