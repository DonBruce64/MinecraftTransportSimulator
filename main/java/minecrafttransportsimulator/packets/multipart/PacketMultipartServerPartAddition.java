package minecrafttransportsimulator.packets.multipart;

import java.lang.reflect.Constructor;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartA_Base;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartServerPartAddition extends APacketMultipartPlayer{
	private byte partIndex;

	public PacketMultipartServerPartAddition(){}
	
	public PacketMultipartServerPartAddition(EntityMultipartA_Base multipart, EntityPlayer player, byte partIndex){
		super(multipart, player);
		this.partIndex = partIndex;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.partIndex=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeByte(this.partIndex);
	}

	public static class Handler implements IMessageHandler<PacketMultipartServerPartAddition, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartServerPartAddition message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					//Check to make sure we can actually add this part before we do so.
					EntityMultipartA_Base multipart = (EntityMultipartA_Base) getMultipart(message, ctx);
					EntityPlayer player = getPlayer(message, ctx);
					
					ItemStack heldStack = player.getHeldItemMainhand();
					if(heldStack != null){
						if(heldStack.getItem() instanceof AItemPart){
							//Player is holding a valid part.  Now check if part goes to this multipart.
							AItemPart partItem = (AItemPart) heldStack.getItem();
							PackPart packPart = multipart.pack.parts.get(message.partIndex);
							for(String partName : packPart.names){
								if(partItem.partName.equals(partName)){
									//This part does go to this multipart.  Now check if one is already there.
									Vec3d partOffset = new Vec3d(packPart.pos[0], packPart.pos[1], packPart.pos[2]);
	        						for(APart part : multipart.getMultipartParts()){
										if(part.offset.equals(partOffset)){
											//Part already exists.  Bail.
											return;
										}
									}
									
									//All clear for adding a new part.  Do so now and tell all clients.
									try{
										Class<? extends APart> partClass = PackParserSystem.getPartPartClass(partItem.partName);
										Constructor<? extends APart> construct = partClass.getConstructor(EntityMultipartD_Moving.class, Vec3d.class, boolean.class, boolean.class, String.class, NBTTagCompound.class);
										APart newPart = construct.newInstance((EntityMultipartD_Moving) multipart, partOffset, packPart.isController, packPart.turnsWithSteer, partName, heldStack.hasTagCompound() ? heldStack.getTagCompound() : new NBTTagCompound());
										multipart.addPart(newPart);
										if(!player.capabilities.isCreativeMode){
											player.inventory.clearMatchingItems(partItem, heldStack.getItemDamage(), 1, heldStack.getTagCompound());
										}
										MTS.MTSNet.sendToAll(new PacketMultipartClientPartAddition(multipart, message.partIndex, heldStack));
									}catch(Exception e){
										MTS.MTSLog.error("ERROR SPAWING PART ON SERVER!");
										MTS.MTSLog.error(e.getMessage());
										e.printStackTrace();
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
