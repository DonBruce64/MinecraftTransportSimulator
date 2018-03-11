package minecrafttransportsimulator.packets.general;

import java.lang.reflect.Constructor;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackPart;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.items.ItemPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MultipartPartAdditionPacket implements IMessage{
	private int id;
	private int player;
	private byte partIndex;

	public MultipartPartAdditionPacket() {}
	
	public MultipartPartAdditionPacket(int id, int player, byte partIndex){
		this.id = id;
		this.player = player;
		this.partIndex = partIndex;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.player=buf.readInt();
		this.partIndex=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeInt(this.player);
		buf.writeByte(this.partIndex);
	}

	public static class Handler implements IMessageHandler<MultipartPartAdditionPacket, IMessage>{
		@Override
		public IMessage onMessage(final MultipartPartAdditionPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					if(ctx.side.isServer()){
						//Check to make sure we can actually add this part before we do so.
						EntityMultipartMoving mover = (EntityMultipartVehicle) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
						EntityPlayer player = (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.player);
						ItemStack heldStack = player.getHeldItemMainhand();
						if(heldStack != null){
							if(heldStack.getItem() instanceof ItemPart){
								//Player is holding a valid part.  Now check if part goes to this multipart.
								ItemPart partItem = (ItemPart) heldStack.getItem();
								PackPart packPart = mover.pack.parts.get(message.partIndex);
								for(String partName : packPart.names){
									if(heldStack.getItem().getRegistryName().getResourcePath().equals(partName)){
										//This part does go to this multipart.  Now check if it fits.
										for(EntityMultipartChild child : mover.getChildren()){
											if(child.offsetX == packPart.pos[0] && child.offsetY == packPart.pos[1] && child.offsetZ == packPart.pos[2]){
												//Part already exists.  Bail.
												return;
											}
										}
										//All clear for adding a new part.  Do so now and tell all clients.
										try{
											Constructor<? extends EntityMultipartChild> construct = partItem.partClassToSpawn.getConstructor(World.class, EntityMultipartParent.class, String.class, float.class, float.class, float.class, int.class);
											EntityMultipartChild newChild = construct.newInstance(mover.worldObj, mover, mover.UUID, packPart.pos[0], packPart.pos[1], packPart.pos[2], heldStack.getItemDamage());
											//TODO make this use the default NBT methods rather than a custom one.
											newChild.setNBTFromStack(heldStack);
											newChild.setTurnsWithSteer(packPart.turnsWithSteer);
											newChild.setController(packPart.isController);
											mover.addChild(newChild.UUID, newChild, true);
											if(!player.capabilities.isCreativeMode){
												player.inventory.clearMatchingItems(heldStack.getItem(), heldStack.getItemDamage(), 1, heldStack.getTagCompound());
											}
										}catch(Exception e){
											MTS.MTSLog.error("ERROR SPAWING PART!");
											e.printStackTrace();
										}
									}
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
