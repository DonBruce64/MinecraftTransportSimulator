package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketPlayer;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Packet used to craft items from crafting benches.  This goes to the server which verifies the
 * player has the appropriate materials.  If so, the item is crafted on the server and materials
 * are deducted if required.  Packet is not sent back to the client as MC will auto-add the item
 * into the player's inventory and will do updates for us.
 * 
 * @author don_bruce
 */
public class PacketPlayerCraftItem extends APacketPlayer{
	private final AItemPack<?> itemToCraft;
	private final boolean forRepair;
	
	public PacketPlayerCraftItem(WrapperPlayer player, AItemPack<?> itemToCraft, boolean forRepair){
		super(player);
		this.itemToCraft = itemToCraft;
		this.forRepair = forRepair;
	}
	
	public PacketPlayerCraftItem(ByteBuf buf){
		super(buf);
		this.itemToCraft = PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf));
		this.forRepair = buf.readBoolean();
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
		buf.writeBoolean(forRepair);
	}
	
	@Override
	public void handle(WrapperWorld world, WrapperPlayer player){
		WrapperInventory inventory = player.getInventory();
		if(player.isCreative() || inventory.hasMaterials(itemToCraft, true, true, forRepair)){
			//If this is for repair, we don't make a new stack, we just use the old stack and a method call.
			if(forRepair){
				//Find the repair item and repair it.
				int repairIndex = inventory.getRepairIndex(itemToCraft);
				WrapperItemStack stack = inventory.getStack(repairIndex);
				AItemPack<?> item = (AItemPack<?>) stack.getItem();
				WrapperNBT stackData = stack.getData();
				item.repair(stackData);
				stack.setData(stackData);
				if(!player.isCreative()){ 
					inventory.removeMaterials(itemToCraft, true, true, forRepair);
				}
				//Need to set stack after item removal, as removal code removes this item.
				inventory.setStack(stack, repairIndex);
			}else{
				//Check we can add the stack before removing materials.
				if(inventory.addStack(itemToCraft.getNewStack(null))){
					if(!player.isCreative()){ 
						inventory.removeMaterials(itemToCraft, true, true, forRepair);
					}
				}
			}
		}
	}
}
