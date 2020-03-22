package minecrafttransportsimulator.packets.parts;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.parts.ItemPartBullet;
import minecrafttransportsimulator.vehicles.parts.APartGun;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPartGunReload extends APacketPart{
	private String bulletPackID;
	private String bulletSystemName;

	public PacketPartGunReload(){}
	
	public PacketPartGunReload(APartGun gun, ItemPartBullet bullet){
		super(gun);
		this.bulletPackID = bullet.definition.packID;
		this.bulletSystemName = bullet.definition.systemName;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.bulletPackID = ByteBufUtils.readUTF8String(buf);
		this.bulletSystemName = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		ByteBufUtils.writeUTF8String(buf, this.bulletPackID);
		ByteBufUtils.writeUTF8String(buf, this.bulletSystemName);
	}

	public static class Handler implements IMessageHandler<PacketPartGunReload, IMessage>{
		public IMessage onMessage(final PacketPartGunReload message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					APartGun gun = (APartGun) getVehiclePartFromMessage(message, ctx);
					if(gun != null){
						gun.loadedBullet = (ItemPartBullet) MTSRegistry.packItemMap.get(message.bulletPackID).get(message.bulletSystemName);
						gun.bulletsLeft += gun.loadedBullet.definition.bullet.quantity;
						gun.reloadTimeRemaining = gun.definition.gun.reloadTime;
						gun.reloading = true;
						MTS.proxy.playSound(Minecraft.getMinecraft().player.getPositionVector(), gun.definition.packID + ":" + gun.definition.systemName + "_reloading", 1, 1, gun.vehicle);
					}
				}
			});
			return null;
		}
	}
}
