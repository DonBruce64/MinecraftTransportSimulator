package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.guis.instances.GUIInstruments;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.items.instances.ItemWrench;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.rendering.components.InterfaceEventsOverlay;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Packet used to change instruments on vehicles.  Sent to the server
 * to process the instrument change, and then sent to all clients if
 * the change is able to be made.  Does not check ownership as that's
 * done before {@link GUIInstruments} is opened by checking for
 * ownership in {@link ItemWrench#doVehicleInteraction}.
 * 
 * @author don_bruce
 */
public class PacketVehicleInstruments extends APacketEntity<EntityVehicleF_Physics>{
	private final int slot;
	private final String instrumentPackID;
	private final String instrumentSystemName;
	
	public PacketVehicleInstruments(EntityVehicleF_Physics vehicle, int slot, ItemInstrument instrument){
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
		this.slot = buf.readInt();
		this.instrumentPackID = readStringFromBuffer(buf);
		this.instrumentSystemName = readStringFromBuffer(buf);
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(slot);
		writeStringToBuffer(instrumentPackID, buf);
		writeStringToBuffer(instrumentSystemName, buf);
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle){
		//Check to make sure the instrument can fit in survival player's inventories.
		//Only check this on the server, as adding things to the client doesn't do us any good.
		if(!world.isClient() && !player.isCreative() && vehicle.instruments.containsKey(slot)){
			ItemInstrument instrument = vehicle.instruments.get(slot);
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
			vehicle.instruments.put(slot, instrument);
		}
		
		//If we are on the client, reset the current HUD.  This prevents load-syncinig issues.
		if(world.isClient()){
			InterfaceEventsOverlay.resetGUI();
		}
		return true;
	}
}
