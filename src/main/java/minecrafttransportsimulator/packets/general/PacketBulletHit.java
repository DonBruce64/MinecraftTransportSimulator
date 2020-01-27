package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.DamageSources.DamageSourceBullet;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.parts.ItemPartBullet;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketBulletHit implements IMessage{
	private double x;
	private double y;
	private double z;
	private double velocity;
	private String bulletPackID;
	private String bulletSystemName;
	private int playerID;
	private int entitiyHitID;

	public PacketBulletHit(){}
	
	public PacketBulletHit(double x, double y, double z, double velocity, ItemPartBullet bullet, int playerID, int entitiyHitID){
		this.x = x;
		this.y = y;
		this.z = z;
		this.velocity = velocity;
		this.bulletPackID = bullet.definition.packID;
		this.bulletSystemName = bullet.definition.systemName;
		this.playerID = playerID;
		this.entitiyHitID = entitiyHitID;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
		this.velocity = buf.readDouble();
		this.bulletPackID = ByteBufUtils.readUTF8String(buf);
		this.bulletSystemName = ByteBufUtils.readUTF8String(buf);
		this.playerID = buf.readInt();
		this.entitiyHitID = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeDouble(this.x);
		buf.writeDouble(this.y);
		buf.writeDouble(this.z);
		buf.writeDouble(this.velocity);
		ByteBufUtils.writeUTF8String(buf, this.bulletPackID);
		ByteBufUtils.writeUTF8String(buf, this.bulletSystemName);
		buf.writeInt(this.playerID);
		buf.writeInt(this.entitiyHitID);
	}
	
	public static class Handler implements IMessageHandler<PacketBulletHit, IMessage>{
		@Override
		public IMessage onMessage(final PacketBulletHit message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					if(ctx.side.isServer()){
						//If we are an explosive bullet, just blow up at our current position.
						//Otherwise do attack logic.
						JSONPart bulletDefinition = (JSONPart) MTSRegistry.packItemMap.get(message.bulletPackID).get(message.bulletSystemName).definition;
						Entity entityAttacking = ctx.getServerHandler().player.world.getEntityByID(message.playerID);
						if(bulletDefinition.bullet.type.equals("explosive")){
							ctx.getServerHandler().player.world.newExplosion(entityAttacking, message.x, message.y, message.z, bulletDefinition.bullet.diameter/10F, false, true);
						}else{
							//If we hit an entity, apply damage to them.
							if(message.entitiyHitID != -1){
								Entity entityHit = ctx.getServerHandler().player.world.getEntityByID(message.entitiyHitID);
								if(entityHit != null && entityAttacking != null){
									//If we are attacking a vehicle, call the custom attack code to relay our position.
									//Otherwise call the regular attack code.
									float damage = (float) (Math.pow(20*message.velocity/100F, 2)*bulletDefinition.bullet.diameter/10F*ConfigSystem.configObject.damage.bulletDamageFactor.value);
									if(entityHit instanceof EntityVehicleB_Existing){
										((EntityVehicleB_Existing) entityHit).attackManuallyAtPosition(message.x, message.y, message.z, new DamageSourceBullet(entityAttacking, bulletDefinition.bullet.type), damage);
									}else{
										entityHit.attackEntityFrom(new DamageSourceBullet(entityAttacking,  bulletDefinition.bullet.type), damage);
										if(bulletDefinition.bullet.type.equals("incendiary")){
											entityHit.setFire(5);
										}
									}
								}
							}else{
								//We didn't hit an entity, so we must have hit a block.
								//If the bullet is big, and the block is soft, then break the block.
								//Otherwise send this packet back to the client to spawn SFX.
								BlockPos hitPos = new BlockPos(message.x, message.y, message.z);
								float hardness = ctx.getServerHandler().player.world.getBlockState(hitPos).getBlockHardness(ctx.getServerHandler().player.world, hitPos);
								if(hardness > 0 && hardness <= (Math.random()*0.3F + 0.3F*bulletDefinition.bullet.diameter/20F)){
									ctx.getServerHandler().player.world.destroyBlock(hitPos, true);
								}else{
									MTS.MTSNet.sendToAll(message);
								}
							}
						}
					}else{
						//We only get a packet back if we hit a block and didn't break it.
						//If this is the case, play the block break sound and spawn some particles.
						BlockPos hitPos = new BlockPos(message.x, message.y, message.z);
						SoundType soundType = Minecraft.getMinecraft().world.getBlockState(hitPos).getBlock().getSoundType(Minecraft.getMinecraft().world.getBlockState(hitPos), Minecraft.getMinecraft().world, hitPos, null);
						Minecraft.getMinecraft().world.playSound(null, message.x, message.y, message.z, soundType.getBreakSound(), SoundCategory.BLOCKS, soundType.getVolume(), soundType.getPitch());
					}
				}
			});
			return null;
		}
	}
}
