package minecrafttransportsimulator.packets.multipart;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.multipart.main.EntityMultipartB_Existing;
import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.packets.general.PacketChat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartAttacked extends APacketMultipartPlayer{

	public PacketMultipartAttacked(){}
	
	public PacketMultipartAttacked(EntityMultipartB_Existing multipart, EntityPlayer player){
		super(multipart, player);
	}

	public static class Handler implements IMessageHandler<PacketMultipartAttacked, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartAttacked message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartB_Existing multipart = (EntityMultipartB_Existing) getMultipart(message, ctx);
					EntityPlayer player = getPlayer(message, ctx);
					
					if(multipart != null && player != null){
						if(player.getHeldItemMainhand() != null && MTSRegistry.wrench.equals(player.getHeldItemMainhand().getItem())){
							if(player.isSneaking()){
								//Attacker is a sneaking player with a wrench.
								//Remove this entity if possible.
								boolean isPlayerOP = player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
								if(multipart.ownerName.isEmpty() || player.getUUID(player.getGameProfile()).toString().equals(multipart.ownerName) || isPlayerOP){
									ItemStack stack = new ItemStack(MTSRegistry.multipartItemMap.get(multipart.multipartName));
									NBTTagCompound stackTag = new NBTTagCompound();
									stackTag.setByte("brokenWindows", multipart.brokenWindows);
									stack.setTagCompound(stackTag);
									multipart.worldObj.spawnEntityInWorld(new EntityItem(multipart.worldObj, multipart.posX, multipart.posY, multipart.posZ, stack));
									multipart.destroyAtPosition(player.posX, player.posY, player.posZ, false);
								}else{
									MTS.MTSNet.sendTo(new PacketChat("interact.failure.vehicleowned"), (EntityPlayerMP) player);
								}
								return;
							}else{
								//Attacker is a player swinging a wrench.
								//If they hit a part, remove it if pemitted.
								APart hitPart = multipart.getHitPart(player);
								if(hitPart != null){
									boolean isPlayerOP = player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
									if(multipart.ownerName.isEmpty() || player.getUUID(player.getGameProfile()).toString().equals(multipart.ownerName) || isPlayerOP){
										//Player can remove part.  Spawn item in the world and remove part.
										//Make sure to remove the part before spawning the item.  Some parts
										//care about this order and won't spawn items unless they've been removed.
										multipart.removePart(hitPart, false);
										Item droppedItem = hitPart.getItemForPart();
										if(droppedItem != null){
											ItemStack droppedStack = new ItemStack(droppedItem);
											droppedStack.setTagCompound(hitPart.getPartNBTTag());
											multipart.worldObj.spawnEntityInWorld(new EntityItem(multipart.worldObj, hitPart.partPos.xCoord, hitPart.partPos.yCoord, hitPart.partPos.zCoord, droppedStack));
										}
									}else{
										MTS.MTSNet.sendTo(new PacketChat("interact.failure.vehicleowned"), (EntityPlayerMP) player);
									}
								}
							}
						}else{
							multipart.attackEntityFrom(DamageSource.causePlayerDamage(player), 1.0F);
						}
					}
				}
			});
			return null;
		}
	}
}
