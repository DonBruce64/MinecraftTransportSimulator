package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.AItemPack;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPlayerCrafting implements IMessage{
	private int playerID;
	private String packID;
	private String systemName;

	public PacketPlayerCrafting(){}
	
	public PacketPlayerCrafting(EntityPlayer player, AItemPack item){
		this.playerID = player.getEntityId();
		this.packID = item.definition.packID;
		this.systemName = item.definition.systemName;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.playerID = buf.readInt();
		this.packID = ByteBufUtils.readUTF8String(buf);
		this.systemName = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.playerID);
		ByteBufUtils.writeUTF8String(buf, this.packID);
		ByteBufUtils.writeUTF8String(buf, this.systemName);
	}
	
	protected static EntityPlayer getPlayer(PacketPlayerCrafting message, MessageContext ctx){
		if(message.playerID != -1){
			if(ctx.side.isServer()){
				return (EntityPlayer) ctx.getServerHandler().player.world.getEntityByID(message.playerID);
			}else{
				return (EntityPlayer) Minecraft.getMinecraft().world.getEntityByID(message.playerID);
			}
		}else{
			return null;
		}
	}
	
	public static boolean doesPlayerHaveMaterials(EntityPlayer player, AItemPack item){
		if(!player.capabilities.isCreativeMode){
			for(ItemStack materialStack : MTSRegistry.getMaterials(item)){
				int requiredMaterialCount = materialStack.getCount();
				for(ItemStack stack : player.inventory.mainInventory){
					if(ItemStack.areItemsEqual(stack, materialStack)){
						requiredMaterialCount -= stack.getCount();
					}
				}
				if(requiredMaterialCount > 0){
					return false;
				}
			}
		}
		return true;
	}
	
	protected static void removeMaterials(EntityPlayer player, AItemPack item){
		if(!player.capabilities.isCreativeMode){
			for(ItemStack materialStack : MTSRegistry.getMaterials(item)){
				player.inventory.clearMatchingItems(materialStack.getItem(), materialStack.getMetadata(), materialStack.getCount(), null);
			}
		}
	}
	
	public static class Handler implements IMessageHandler<PacketPlayerCrafting, IMessage>{
		public IMessage onMessage(final PacketPlayerCrafting message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityPlayer player = getPlayer(message, ctx);
					if(player != null){
						AItemPack item = MTSRegistry.packItemMap.get(message.packID).get(message.systemName);
						if(doesPlayerHaveMaterials(player, item)){
							removeMaterials(player, item);
							player.getEntityWorld().spawnEntity(new EntityItem(player.getEntityWorld(), player.posX, player.posY, player.posZ, new ItemStack(item)));
						}
					}
				}
			});
			return null;
		}
	}
}
