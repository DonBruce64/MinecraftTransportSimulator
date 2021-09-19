package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.guis.instances.GUIInventoryContainer;
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketPlayer;
import minecrafttransportsimulator.packets.components.InterfacePacket;

/**Packet used to sync hand-held interactable crate inventory.  This is first sent by the server to the client
 * to open the crate GUI.  Once the client closes the GUI, this packet is sent back to the server to update
 * the item data with whatever changes the player made.
 * 
 * @author don_bruce
 */
public class PacketItemInteractable extends APacketPlayer{
	private final int lookupID;
	private final WrapperNBT data;
	private final int units;
	private final String texture;
	
	public PacketItemInteractable(WrapperPlayer player, EntityInventoryContainer inventory, String texture){
		super(player);
		this.lookupID = inventory.lookupID;
		this.data = inventory.save(new WrapperNBT());
		this.units = inventory.getSize();
		this.texture = texture;
	}
	
	private PacketItemInteractable(WrapperPlayer player, int lookupID){
		super(player);
		this.lookupID = lookupID;
		this.data = null;
		this.units = 0;
		this.texture = null;
	}
	
	public PacketItemInteractable(ByteBuf buf){
		super(buf);
		this.lookupID = buf.readInt();
		if(buf.readBoolean()){
			this.data = readDataFromBuffer(buf);
			this.units = buf.readInt();
			if(buf.readBoolean()){
				this.texture = readStringFromBuffer(buf);
			}else{
				this.texture = null;
			}
		}else{
			this.data = null;
			this.units = 0;
			this.texture = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(lookupID);
		if(data == null){
			buf.writeBoolean(false);
		}else{
			buf.writeBoolean(true);
			writeDataToBuffer(data, buf);
			buf.writeInt(units);
			if(texture != null){
				buf.writeBoolean(true);
				writeStringToBuffer(texture, buf);
			}else{
				buf.writeBoolean(false);
			}
		}
	}
	
	@Override
	public void handle(WrapperWorld world, WrapperPlayer player){
		if(world.isClient()){
			EntityInventoryContainer inventory = new EntityInventoryContainer(world, data, units);
			InterfaceGUI.openGUI(new GUIInventoryContainer(inventory, texture){
				@Override
				public void onClosed(){
					//Sends a packet back to the server to have it save state.
					//Also kills the inventory to prevent memory leaks.
					InterfacePacket.sendToServer(new PacketItemInteractable(player, lookupID));
					inventory.remove();
				}
			});
		}else{
			EntityInventoryContainer inventory = AEntityA_Base.getEntity(world, lookupID);
			WrapperNBT newData = new WrapperNBT();
			newData.setData("inventory", inventory.save(new WrapperNBT()));
			player.getHeldStack().setTagCompound(newData.tag);
			inventory.remove();
		}
	}
}
