package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PlayerCraftingPacket implements IMessage{
	private int playerID;
	private String selectedItem;

	public PlayerCraftingPacket(){}
	
	public PlayerCraftingPacket(EntityPlayer player, String selectedItem){
		this.playerID = player.getEntityId();
		this.selectedItem = selectedItem;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.playerID = buf.readInt();
		this.selectedItem = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.playerID);
		ByteBufUtils.writeUTF8String(buf, this.selectedItem);
	}
	
	protected static EntityPlayer getPlayer(PlayerCraftingPacket message, MessageContext ctx){
		if(message.playerID != -1){
			if(ctx.side.isServer()){
				return (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.playerID);
			}else{
				return (EntityPlayer) Minecraft.getMinecraft().theWorld.getEntityByID(message.playerID);
			}
		}else{
			return null;
		}
	}
	
	public static boolean doesPlayerHaveMaterials(EntityPlayer player, String partToCraft){
		if(!player.capabilities.isCreativeMode){
			for(ItemStack materialStack : PackParserSystem.getMaterials(partToCraft)){
				int requiredMaterialCount = materialStack.stackSize;
				for(ItemStack stack : player.inventory.mainInventory){
					if(ItemStack.areItemsEqual(stack, materialStack)){
						requiredMaterialCount -= stack.stackSize;
					}
				}
				if(requiredMaterialCount > 0){
					return false;
				}
			}
		}
		return true;
	}
	
	protected static void removeMaterials(EntityPlayer player, String partToCraft){
		if(!player.capabilities.isCreativeMode){
			for(ItemStack materialStack : PackParserSystem.getMaterials(partToCraft)){
				player.inventory.clearMatchingItems(materialStack.getItem(), materialStack.getMetadata(), materialStack.stackSize, null);
			}
		}
	}
	
	public static class Handler implements IMessageHandler<PlayerCraftingPacket, IMessage>{
		public IMessage onMessage(final PlayerCraftingPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityPlayer player = getPlayer(message, ctx);
					if(player != null){
						if(doesPlayerHaveMaterials(player, message.selectedItem)){
							removeMaterials(player, message.selectedItem);
							ItemStack stack;
							if(MTSRegistry.multipartItemMap.containsKey(message.selectedItem)){
								stack = new ItemStack(MTSRegistry.multipartItemMap.get(message.selectedItem));
							}else if(MTSRegistry.partItemMap.containsKey(message.selectedItem)){
								stack = new ItemStack(MTSRegistry.partItemMap.get(message.selectedItem));
							}else{
								stack = new ItemStack(MTSRegistry.instrumentItemMap.get(message.selectedItem));
							}
							player.getEntityWorld().spawnEntityInWorld(new EntityItem(player.getEntityWorld(), player.posX, player.posY, player.posZ, stack));
						}
					}
				}
			});
			return null;
		}
	}
}
