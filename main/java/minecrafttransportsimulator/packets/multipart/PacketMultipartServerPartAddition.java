package minecrafttransportsimulator.packets.multipart;

import java.lang.reflect.Constructor;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartA_Base;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartServerPartAddition extends APacketMultipartPart{
	private int player;

	public PacketMultipartServerPartAddition(){}
	
	public PacketMultipartServerPartAddition(EntityMultipartA_Base multipart, double offsetX, double offsetY, double offsetZ, EntityPlayer player){
		super(multipart, offsetX, offsetY, offsetZ);
		this.player = player.getEntityId();
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.player = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeInt(this.player);
	}

	public static class Handler implements IMessageHandler<PacketMultipartServerPartAddition, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartServerPartAddition message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					//Check to make sure we can actually add this part before we do so.
					EntityMultipartA_Base multipart = (EntityMultipartA_Base) getMultipart(message, ctx);
					EntityPlayer player;
					if(ctx.side.isServer()){
						player = (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.player);
					}else{
						player = (EntityPlayer) Minecraft.getMinecraft().theWorld.getEntityByID(message.player);
					}
					
					ItemStack heldStack = player.getHeldItemMainhand();
					if(heldStack != null){
						if(heldStack.getItem() instanceof AItemPart){
							//Player is holding a valid part.  Now check if part goes to this multipart.
							AItemPart partItem = (AItemPart) heldStack.getItem();
							PackPart packPart = multipart.getPackDefForLocation(message.offsetX, message.offsetY, message.offsetZ);
							PackPartObject itemPack = PackParserSystem.getPartPack(partItem.partName);
							if(packPart.types.contains(itemPack.general.type)){
								//This part does go to this multipart.  Now check if one is already there.
								Vec3d partOffset = new Vec3d(message.offsetX, message.offsetY, message.offsetZ);
        						for(APart part : multipart.getMultipartParts()){
									if(part.offset.equals(partOffset)){
										//Part already exists.  Bail.
										return;
									}
								}
        						PackParserSystem.getPartPack(partItem.partName);
        						if(!partItem.isPartValidForPackDef(packPart)){
    								//Part is a valid type, but is not a valid configuration.  Bail.
        							return;
    							}
								
								//All clear for adding a new part.  Do so now and tell all clients.
								try{
									Class<? extends APart> partClass = PackParserSystem.getPartPartClass(partItem.partName);
									Constructor<? extends APart> construct = partClass.getConstructor(EntityMultipartD_Moving.class, Vec3d.class, boolean.class, boolean.class, String.class, NBTTagCompound.class);
									APart newPart = construct.newInstance((EntityMultipartD_Moving) multipart, partOffset, packPart.isController, packPart.turnsWithSteer, partItem.partName, heldStack.hasTagCompound() ? heldStack.getTagCompound() : new NBTTagCompound());
									multipart.addPart(newPart, false);
									if(!player.capabilities.isCreativeMode){
										player.inventory.clearMatchingItems(partItem, heldStack.getItemDamage(), 1, heldStack.getTagCompound());
									}
									MTS.MTSNet.sendToAll(new PacketMultipartClientPartAddition(multipart, message.offsetX, message.offsetY, message.offsetZ, heldStack));
								}catch(Exception e){
									MTS.MTSLog.error("ERROR SPAWING PART ON SERVER!");
									MTS.MTSLog.error(e.getMessage());
									e.printStackTrace();
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
