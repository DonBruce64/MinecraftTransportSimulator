package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.items.instances.ItemPartInteractable;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**Packet used to send transfer an item to or from a player inventory to an inventory in a
 * {@link EntityInventoryContainer}.  While containers have their own packets for this, player
 * inventory is handled by MC, and does not follow the same architecture.  However, it does sync
 * on changes like ours, so we don't need to send a client packet back for either.
 * 
 * @author don_bruce
 */
public class PacketPlayerItemTransfer extends APacketEntityInteract<EntityInventoryContainer, WrapperPlayer>{
	private final int inventorySlot;
	private final int playerSlot;
	private final boolean saveToPlayer;
	
	public PacketPlayerItemTransfer(EntityInventoryContainer inventory, WrapperPlayer player, int inventorySlot, int playerSlot, boolean saveToPlayer){
		super(inventory, player);
		this.inventorySlot = inventorySlot;
		this.playerSlot = playerSlot;
		this.saveToPlayer = saveToPlayer;
	}
	
	public PacketPlayerItemTransfer(ByteBuf buf){
		super(buf);
		this.inventorySlot = buf.readInt();
		this.playerSlot = buf.readInt();
		this.saveToPlayer = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(inventorySlot);
		buf.writeInt(playerSlot);
		buf.writeBoolean(saveToPlayer);
	}
	
	@Override
	public boolean handle(WrapperWorld world, EntityInventoryContainer inventory, WrapperPlayer player){
		WrapperInventory playerInventory = player.getInventory();
		if(inventorySlot != -1){
			WrapperItemStack stackToTransfer = inventory.getStack(inventorySlot);
			if(playerInventory.addStack(stackToTransfer)){
				inventory.setStack(stackToTransfer, inventorySlot);
			}
		}else if(playerSlot != -1){
			WrapperItemStack stackToTransfer = playerInventory.getStack(playerSlot);
			
			//Make sure we aren't an inventory container.
			//Those can't go into our inventories.
			AItemBase item = stackToTransfer.getItem();
			if(item instanceof ItemPartInteractable && ((ItemPartInteractable) item).definition.interactable.inventoryUnits != 0){
				return false;
			}
			if(item instanceof ItemDecor && ((ItemDecor) item).definition.decor.inventoryUnits != 0){
				return false;
			}
			
			if(inventory.addStack(stackToTransfer)){
				playerInventory.setStack(stackToTransfer, playerSlot);
			}
		}
		if(saveToPlayer){
			WrapperNBT newData = new WrapperNBT();
			newData.setData("inventory", inventory.save(new WrapperNBT()));
			player.getHeldStack().setData(newData);
		}
		return false;
	}
}
