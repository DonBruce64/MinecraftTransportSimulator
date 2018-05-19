package minecrafttransportsimulator.packets.multipart;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.multipart.main.EntityMultipartB_Existing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartWindowFix extends APacketMultipartPlayer{

	public PacketMultipartWindowFix(){}
	
	public PacketMultipartWindowFix(EntityMultipartB_Existing multipart, EntityPlayer player){
		super(multipart, player);
	}

	public static class Handler implements IMessageHandler<PacketMultipartWindowFix, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartWindowFix message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartB_Existing multipart = (EntityMultipartB_Existing) getMultipart(message, ctx);
					EntityPlayer player = getPlayer(message, ctx);
					
					ItemStack heldStack = player.getHeldItemMainhand();
					if(heldStack != null){
						if(Item.getItemFromBlock(Blocks.GLASS_PANE).equals(heldStack.getItem())){
							if(multipart.brokenWindows > 0){
								if(!player.capabilities.isCreativeMode && ctx.side.isServer()){
									player.inventory.clearMatchingItems(Item.getItemFromBlock(Blocks.GLASS_PANE), 0, 1, null);
								}
								--multipart.brokenWindows;
								if(ctx.side.isServer()){
									MTS.MTSNet.sendToAll(message);
								}
							}
						}
					}
				}
			});
			return null;
		}
	}
}
