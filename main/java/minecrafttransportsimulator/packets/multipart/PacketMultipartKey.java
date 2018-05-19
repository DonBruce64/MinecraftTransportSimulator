package minecrafttransportsimulator.packets.multipart;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.core.ItemKey;
import minecrafttransportsimulator.multipart.main.EntityMultipartB_Existing;
import minecrafttransportsimulator.packets.general.ChatPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartKey extends APacketMultipartPlayer{
	
	public PacketMultipartKey(){}
	
	public PacketMultipartKey(EntityMultipartB_Existing multipart, EntityPlayer player){
		super(multipart, player);
	}
	
	public static class Handler implements IMessageHandler<PacketMultipartKey, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartKey message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartB_Existing multipart = (EntityMultipartB_Existing) getMultipart(message, ctx);
					EntityPlayer player = getPlayer(message, ctx);
					
					String messageString = "";
					ItemStack heldStack = player.getHeldItemMainhand();
					if(heldStack != null){
						if(MTSRegistry.key.equals(heldStack.getItem())){
							ItemKey key = (ItemKey) heldStack.getItem();
							if(player.isSneaking()){
								//If player is sneaking with a key we need to change ownership.
								messageString = changeOwner(heldStack, multipart, player);
							}else{
								//Not sneaking, do locking code.
								messageString = changeLock(heldStack, multipart, player);
							}
							if(ctx.side.isServer()){
								MTS.MTSNet.sendToAll(message);
								if(!messageString.isEmpty()){ 
									MTS.MTSNet.sendTo(new ChatPacket(messageString), (EntityPlayerMP) player);
								}
							}
						}
					}
				}
			});
			return null;
		}
		
		private static String changeOwner(ItemStack stack, EntityMultipartB_Existing multipart, EntityPlayer player){
			if(multipart.ownerName.isEmpty()){
				//No owner, take ownership.
				multipart.ownerName = player.getUUID(player.getGameProfile()).toString();
				return "interact.key.info.own";
			}else{
				//Already owned, check to see if we can disown.
				boolean isPlayerOP = player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
				if(player.getUUID(player.getGameProfile()).toString().equals(multipart.ownerName) || isPlayerOP){
					multipart.ownerName = "";
					return "interact.key.info.unown";	
				}else{
					return "interact.key.failure.alreadyowned";
				}
			}
		}
		
		private static String changeLock(ItemStack stack, EntityMultipartB_Existing multipart, EntityPlayer player){
			String vehicleUUID = stack.hasTagCompound() ? stack.getTagCompound().getString("vehicle") : "";
			if(vehicleUUID.isEmpty()){
				if(!multipart.ownerName.isEmpty()){
					if(!player.getUUID(player.getGameProfile()).toString().equals(multipart.ownerName)){
						return "interact.key.failure.notowner";
					}
				}
				NBTTagCompound tag = new NBTTagCompound();
				tag.setString("vehicle", multipart.getUniqueID().toString());
				stack.setTagCompound(tag);
				
				multipart.locked = true;
				return "interact.key.info.lock";
			}else if(!vehicleUUID.equals(multipart.getUniqueID().toString())){
				return "interact.key.failure.wrongkey";
			}else{
				if(multipart.locked){
					multipart.locked = false;
					return "interact.key.info.unlock";
				}else{
					multipart.locked = true;
					return "interact.key.info.lock";
				}
			}
		}
	}
}
