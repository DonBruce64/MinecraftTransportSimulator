package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSAchievements;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.items.ItemKey;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MultipartKeyActionPacket implements IMessage{
	private int id;
	private int player;

	public MultipartKeyActionPacket() {}
	
	public MultipartKeyActionPacket(int id, int player){
		this.id = id;
		this.player = player;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.player=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeInt(this.player);
	}

	public static class Handler implements IMessageHandler<MultipartKeyActionPacket, IMessage>{
		@Override
		public IMessage onMessage(final MultipartKeyActionPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartMoving mover;
					EntityPlayer player;
					if(ctx.side.isServer()){
						mover = (EntityMultipartMoving) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
						player = (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.player);
					}else{
						mover = (EntityMultipartMoving) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
						player = (EntityPlayer) Minecraft.getMinecraft().theWorld.getEntityByID(message.player);
					}
					
					String messageString = "";
					ItemStack heldStack = player.getHeldItemMainhand();
					if(heldStack != null){
						if(MTSRegistry.key.equals(heldStack.getItem())){
							ItemKey key = (ItemKey) heldStack.getItem();
							if(player.isSneaking()){
								//If player is sneaking with a key we need to change ownership.
								messageString = changeOwner(heldStack, mover, player);
							}else{
								//Not sneaking, do locking code.
								messageString = changeLock(heldStack, mover, player);
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
		
		private static String changeOwner(ItemStack stack, EntityMultipartMoving mover, EntityPlayer player){
			if(mover.ownerName.isEmpty()){
				//No owner, take ownership.
				mover.ownerName = player.getUUID(player.getGameProfile()).toString();
				return "interact.key.info.own";
			}else{
				//Already owned, check to see if we can disown.
				boolean isPlayerOP = player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
				if(player.getUUID(player.getGameProfile()).toString().equals(mover.ownerName) || isPlayerOP){
					mover.ownerName = "";
					return "interact.key.info.unown";	
				}else{
					return "interact.key.failure.alreadyowned";
				}
			}
		}
		
		private static String changeLock(ItemStack stack, EntityMultipartMoving mover, EntityPlayer player){
			String vehicleUUID = stack.hasTagCompound() ? stack.getTagCompound().getString("vehicle") : "";
			if(vehicleUUID.isEmpty()){
				if(!mover.ownerName.isEmpty()){
					if(!player.getUUID(player.getGameProfile()).toString().equals(mover.ownerName)){
						return "interact.key.failure.notowner";
					}
				}
				NBTTagCompound tag = new NBTTagCompound();
				tag.setString("vehicle", mover.UUID);
				stack.setTagCompound(tag);
				
				mover.locked = true;
				MTSAchievements.triggerKey(player);
				return "interact.key.info.lock";
			}else if(!vehicleUUID.equals(mover.UUID)){
				return "interact.key.failure.wrongkey";
			}else{
				if(mover.locked){
					mover.locked = false;
					return "interact.key.info.unlock";
				}else{
					mover.locked = true;
					return "interact.key.info.lock";
				}
			}
		}
	}
}
