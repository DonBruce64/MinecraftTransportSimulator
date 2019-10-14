package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.mcinterface.MTSNetwork;
import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**This packet is sent to servers when a player tries
 * to modify the instruments of a vehicle from the
 * instrument GUI.  When the server gets this packet, it
 * will check to make sure the player does indeed have the
 * instrument they clicked, and, if an instrument is being
 * removed, that the player can actually store that instrument
 * in their inventory.  If the operation is successful, the packet
 * is sent to all clients tracking the vehicle so they can update
 * the instrument on their side.
 * 
 * @author don_bruce
 */
public class PacketVehicleCommonInstruments extends APacketVehiclePlayer{
	private byte slotToChange;
	private String instrumentToChangeTo;

	public PacketVehicleCommonInstruments(){}
	
	public PacketVehicleCommonInstruments(EntityVehicleE_Powered vehicle, int playerID, byte slotToChange, String instrumentToChangeTo){
		super(vehicle, playerID);
		this.slotToChange = slotToChange;
		this.instrumentToChangeTo = instrumentToChangeTo;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		slotToChange = tag.getByte("slotToChange");
		instrumentToChangeTo = tag.getString("instrumentToChangeTo");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setByte("slotToChange", slotToChange);
		tag.setString("instrumentToChangeTo", instrumentToChangeTo);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		EntityVehicleE_Powered vehicle = (EntityVehicleE_Powered) getVehicle(world);
		MTSPlayerInterface player = getPlayer(world);
		if(vehicle != null && (player != null || !onServer)){
			if(onServer){
				//Check to make sure the instrument can fit in survival player's inventories.
				if(!player.creative() && onServer && vehicle.getInstrumentInfoInSlot(slotToChange) != null){
					if(!player.addStack(new ItemStack(MTSRegistry.instrumentItemMap.get(vehicle.getInstrumentInfoInSlot(slotToChange).name)))){
						return;
					}
				}
				
				//Check to make sure player has the instrument they are trying to put in.
				if(!player.creative() && onServer && !instrumentToChangeTo.isEmpty()){
					if(player.hasItems(MTSRegistry.instrumentItemMap.get(instrumentToChangeTo), 0, 0)){
						player.removeItems(MTSRegistry.instrumentItemMap.get(instrumentToChangeTo), 1, -1);
					}else{
						return;
					}
				}
				MTSNetwork.sendPacketToPlayersTracking(this, vehicle);
			}
			vehicle.setInstrumentInSlot(slotToChange, instrumentToChangeTo);
		}
	}
}
