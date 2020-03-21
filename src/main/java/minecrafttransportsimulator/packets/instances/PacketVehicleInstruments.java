package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.guis.instances.GUIInstruments;
import minecrafttransportsimulator.items.core.ItemWrench;
import minecrafttransportsimulator.items.packs.ItemInstrument;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraft.item.ItemStack;

/**Packet used to change instruments on vehicles.  Sent to the server
 * to process the instrument change, and then sent to all clients if
 * the change is able to be made.  Does not check ownership as that's
 * done before {@link GUIInstruments} is opened by checking for
 * ownership in {@link ItemWrench#doVehicleInteraction}.
 * 
 * @author don_bruce
 */
public class PacketVehicleInstruments extends APacketVehicle{
	private byte slot;
	private String instrumentPackID;
	private String instrumentSystemName;
	
	public PacketVehicleInstruments(EntityVehicleE_Powered vehicle, byte slot, ItemInstrument instrument){
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
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleE_Powered vehicle){
		//Check to make sure the instrument can fit in survival player's inventories.
		//Only check this on the server, as adding things to the client doesn't do us any good.
		if(!world.isClient() && !player.isCreative() && vehicle.instruments.containsKey(slot)){
			if(!player.addItem(new ItemStack(vehicle.instruments.get(slot)))){
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
			ItemInstrument instrument = (ItemInstrument) MTSRegistry.packItemMap.get(instrumentPackID).get(instrumentSystemName);
			if(!world.isClient() && !player.isCreative()){
				if(player.hasItem(instrument, 1, 0)){
					player.removeItem(new ItemStack(instrument));
				}else{
					return false;
				}
			}
			vehicle.instruments.put(slot, instrument);
		}
		return true;
	}
}
