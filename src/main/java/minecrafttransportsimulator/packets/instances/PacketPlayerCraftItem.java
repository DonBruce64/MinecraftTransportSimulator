package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Packet used to craft items from crafting benches.  This goes to the server which verifies the
 * player has the appropriate materials.  If so, the item is crafted on the server and materials
 * are deducted if required.  Packet is not sent back to the client as MC will auto-add the item
 * into the player's inventory and will do updates for us.
 * 
 * @author don_bruce
 */
public class PacketPlayerCraftItem extends APacketBase{
	private final AItemPack<? extends AJSONItem<?>> itemToCraft;
	
	public PacketPlayerCraftItem(AItemPack<? extends AJSONItem<?>> itemToCraft){
		super(null);
		this.itemToCraft = itemToCraft;
	}
	
	public PacketPlayerCraftItem(ByteBuf buf){
		super(buf);
		this.itemToCraft = PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf));
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(itemToCraft.definition.packID, buf);
		if(itemToCraft instanceof AItemSubTyped){
			writeStringToBuffer(itemToCraft.definition.systemName + ((AItemSubTyped<?>) itemToCraft).subName, buf);
		}else{
			writeStringToBuffer(itemToCraft.definition.systemName, buf);
		}
	}
	
	@Override
	public void handle(IWrapperWorld world, IWrapperPlayer player){
		IWrapperInventory inventory = player.getInventory();
		if(player.isCreative() || inventory.hasMaterials(itemToCraft)){
			if(!player.isCreative()){
				inventory.removeMaterials(itemToCraft);
			}
			inventory.addItem(itemToCraft, null);
		}
	}
}
