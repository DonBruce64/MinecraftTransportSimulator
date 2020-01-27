package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPackReload implements IMessage{
	public PacketPackReload(){}
	
	@Override
	public void fromBytes(ByteBuf buf){}

	@Override
	public void toBytes(ByteBuf buf){}

	public static class Handler implements IMessageHandler<PacketPackReload, IMessage>{
		@Override
		public IMessage onMessage(final PacketPackReload message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					for(Entity entity : ctx.getServerHandler().player.world.loadedEntityList){
						if(entity instanceof EntityVehicleA_Base){
							EntityVehicleA_Base vehicle = (EntityVehicleA_Base) entity;
							vehicle.definition = (JSONVehicle) MTSRegistry.packItemMap.get(vehicle.definition.packID).get(vehicle.definition.systemName).definition;
						}
					}
				}
			});
			return null;
		}
	}

}
