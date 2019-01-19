package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleWindowFix extends APacketVehiclePlayer{

	public PacketVehicleWindowFix(){}
	
	public PacketVehicleWindowFix(EntityVehicleB_Existing vehicle, EntityPlayer player){
		super(vehicle, player);
	}

	public static class Handler implements IMessageHandler<PacketVehicleWindowFix, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleWindowFix message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleB_Existing vehicle = (EntityVehicleB_Existing) getVehicle(message, ctx);
					EntityPlayer player = getPlayer(message, ctx);
					
					ItemStack heldStack = player.getHeldItemMainhand();
					if(heldStack != null){
						if(Item.getItemFromBlock(Blocks.GLASS_PANE).equals(heldStack.getItem())){
							if(vehicle.brokenWindows > 0){
								if(!player.capabilities.isCreativeMode && ctx.side.isServer()){
									player.inventory.clearMatchingItems(Item.getItemFromBlock(Blocks.GLASS_PANE), 0, 1, null);
								}
								--vehicle.brokenWindows;
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
