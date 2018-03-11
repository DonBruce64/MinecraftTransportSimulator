package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MultipartNameTagActionPacket implements IMessage{
	private int id;
	private int player;

	public MultipartNameTagActionPacket() {}
	
	public MultipartNameTagActionPacket(int id, int player){
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

	public static class Handler implements IMessageHandler<MultipartNameTagActionPacket, IMessage>{
		@Override
		public IMessage onMessage(final MultipartNameTagActionPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartMoving mover;
					EntityPlayer player;
					if(ctx.side.isServer()){
						mover = (EntityMultipartMoving) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
						player = (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.player);
					}else{
						mover = (EntityMultipartMoving) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
						player = (EntityPlayer) Minecraft.getMinecraft().theWorld.getEntityByID(message.player);
					}
					
					ItemStack heldStack = player.getHeldItemMainhand();
					if(heldStack != null){
						if(Items.NAME_TAG.equals(heldStack.getItem())){
							int maxText = mover.pack.general.displayTextMaxLength;
							mover.displayText = heldStack.getDisplayName().length() > maxText ? heldStack.getDisplayName().substring(0, maxText - 1) : heldStack.getDisplayName();
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
