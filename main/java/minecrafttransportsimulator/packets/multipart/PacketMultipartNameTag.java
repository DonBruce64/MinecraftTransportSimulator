package minecrafttransportsimulator.packets.multipart;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.multipart.main.EntityMultipartB_Existing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartNameTag extends APacketMultipartPlayer{

	public PacketMultipartNameTag(){}
	
	public PacketMultipartNameTag(EntityMultipartB_Existing multipart, EntityPlayer player){
		super(multipart, player);
	}

	public static class Handler implements IMessageHandler<PacketMultipartNameTag, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartNameTag message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartB_Existing multipart = (EntityMultipartB_Existing) getMultipartFromMessage(message, ctx);
					EntityPlayer player = getPlayerFromMessage(message, ctx);
					
					ItemStack heldStack = player.getHeldItemMainhand();
					if(heldStack != null){
						if(Items.NAME_TAG.equals(heldStack.getItem())){
							int maxText = multipart.pack.rendering.displayTextMaxLength;
							multipart.displayText = heldStack.getDisplayName().length() > maxText ? heldStack.getDisplayName().substring(0, maxText - 1) : heldStack.getDisplayName();
							if(ctx.side.isServer()){
								MTS.MTSNet.sendToAll(message);
							}
						}
					}
				}
			});
			return null;
		}
	}
}
