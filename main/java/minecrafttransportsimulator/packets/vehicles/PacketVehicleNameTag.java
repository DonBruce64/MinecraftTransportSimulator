package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleNameTag extends APacketVehiclePlayer{

	public PacketVehicleNameTag(){}
	
	public PacketVehicleNameTag(EntityVehicleB_Existing vehicle, EntityPlayer player){
		super(vehicle, player);
	}

	public static class Handler implements IMessageHandler<PacketVehicleNameTag, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleNameTag message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleB_Existing vehicle = (EntityVehicleB_Existing) getVehicle(message, ctx);
					EntityPlayer player = getPlayer(message, ctx);
					
					ItemStack heldStack = player.getHeldItemMainhand();
					if(heldStack != null){
						if(Items.NAME_TAG.equals(heldStack.getItem())){
							int maxText = vehicle.pack.rendering.displayTextMaxLength;
							vehicle.displayText = heldStack.getDisplayName().length() > maxText ? heldStack.getDisplayName().substring(0, maxText - 1) : heldStack.getDisplayName();
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
