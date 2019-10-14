package minecrafttransportsimulator.packets.parts;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackPartObject.PartBulletConfig;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.parts.APartGun;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPartGunReload extends APacketPart{
	private String bulletReloaded;

	public PacketPartGunReload(){}
	
	public PacketPartGunReload(APartGun gun, String bulletReloaded){
		super(gun);
		this.bulletReloaded = bulletReloaded;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.bulletReloaded = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		ByteBufUtils.writeUTF8String(buf, this.bulletReloaded);
	}

	public static class Handler implements IMessageHandler<PacketPartGunReload, IMessage>{
		public IMessage onMessage(final PacketPartGunReload message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					APartGun gun = (APartGun) getPartFromMessage(message, ctx);
					if(gun != null){
						PartBulletConfig bulletPack = PackParserSystem.getPartPack(message.bulletReloaded).bullet;
						gun.loadedBullet = message.bulletReloaded;
						gun.bulletsLeft += bulletPack.quantity;
						gun.reloadTimeRemaining = gun.pack.gun.reloadTime;
						gun.reloading = true;
						MTS.proxy.playSound(Minecraft.getMinecraft().player.getPositionVector(), gun.partName + "_reloading", 1, 1);
					}
				}
			});
			return null;
		}
	}
}
