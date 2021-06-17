package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;
import net.minecraft.item.ItemStack;

/**Packet used to send transfer an item to or from a player inventory to an inventory in a
 * {@link EntityInventoryContainer}.  While containers have their own packets for this, player
 * inventory is handled by MC, and does not follow the same architecture.
 * 
 * @author don_bruce
 */
public class PacketPlayerItemTransfer extends APacketEntityInteract<EntityInventoryContainer, WrapperPlayer>{
	private final int inventorySlot;
	private final int playerSlot;
	
	public PacketPlayerItemTransfer(EntityInventoryContainer inventory, WrapperPlayer player, int inventorySlot, int playerSlot){
		super(inventory, player);
		this.inventorySlot = inventorySlot;
		this.playerSlot = playerSlot;
	}
	
	public PacketPlayerItemTransfer(ByteBuf buf){
		super(buf);
		this.inventorySlot = buf.readInt();
		this.playerSlot = buf.readInt();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(inventorySlot);
		buf.writeInt(playerSlot);
	}
	
	@Override
	public boolean handle(WrapperWorld world, EntityInventoryContainer inventory, WrapperPlayer player){
		if(inventorySlot != -1){
			if(player.getInventory().addStack(inventory.getStack(inventorySlot))){
				inventory.setStack(ItemStack.EMPTY, inventorySlot);
			}
		}else if(playerSlot != -1){
			WrapperInventory playerInventory = player.getInventory();
			playerInventory.decrementSlot(playerSlot, inventory.addStack(playerInventory.getStackInSlot(playerSlot), true));
		}
		return false;
	}
}
