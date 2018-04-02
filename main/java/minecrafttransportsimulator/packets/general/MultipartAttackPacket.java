package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MultipartAttackPacket implements IMessage{
	private int id;
	private int player;

	public MultipartAttackPacket() {}
	
	public MultipartAttackPacket(int id, int player){
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

	public static class Handler implements IMessageHandler<MultipartAttackPacket, IMessage>{
		@Override
		public IMessage onMessage(final MultipartAttackPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartMoving mover = (EntityMultipartMoving) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
					EntityPlayer player = (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.player);
					
					if(mover != null && player != null){
						if(player.getHeldItemMainhand() != null && MTSRegistry.wrench.equals(player.getHeldItemMainhand().getItem())){
							if(player.isSneaking()){
								//Attacker is a sneaking player with a wrench.
								//Remove this entity if possible.
								boolean isPlayerOP = player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
								if(mover.ownerName.isEmpty() || player.getUUID(player.getGameProfile()).toString().equals(mover.ownerName) || isPlayerOP){
									ItemStack stack = new ItemStack(MTSRegistry.multipartItemMap.get(mover.name));
									NBTTagCompound stackTag = new NBTTagCompound();
									stackTag.setByte("brokenWindows", mover.brokenWindows);
									stack.setTagCompound(stackTag);
									mover.worldObj.spawnEntityInWorld(new EntityItem(mover.worldObj, mover.posX, mover.posY, mover.posZ, stack));
									mover.setDead();
								}else{
									MTS.MTSNet.sendTo(new ChatPacket("interact.failure.vehicleowned"), (EntityPlayerMP) player);
								}
								return;
							}else{
								//Attacker is a player swinging a wrench.
								//If they hit a part, remove it if pemitted.
								EntityMultipartChild hitChild = mover.getHitChild(player);
								if(hitChild != null){
									boolean isPlayerOP = player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
									if(mover.ownerName.isEmpty() || player.getUUID(player.getGameProfile()).toString().equals(mover.ownerName) || isPlayerOP){
										//Player can remove part.  Spawn item in the world and remove part.
										ItemStack droppedItem = hitChild.getItemStack();
										if(droppedItem != null){
											mover.worldObj.spawnEntityInWorld(new EntityItem(mover.worldObj, hitChild.posX, hitChild.posY, hitChild.posZ, droppedItem));
										}
										mover.removeChild(hitChild.UUID, false);
									}else{
										MTS.MTSNet.sendTo(new ChatPacket("interact.failure.vehicleowned"), (EntityPlayerMP) player);
									}
								}
							}
						}else{
							mover.attackEntityFrom(DamageSource.causePlayerDamage(player), 1.0F);
						}
					}
				}
			});
			return null;
		}
	}
}
