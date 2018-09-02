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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartClientPartAddition extends APacketMultipartPart{
	private ItemStack partStack;

	public PacketMultipartClientPartAddition(){}
	
	public PacketMultipartClientPartAddition(EntityMultipartA_Base multipart, double offsetX, double offsetY, double offsetZ, ItemStack partStack){
		super(multipart, offsetX, offsetY, offsetZ);
		this.partStack = partStack;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.partStack=ByteBufUtils.readItemStack(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		ByteBufUtils.writeItemStack(buf, partStack);
	}

	public static class Handler implements IMessageHandler<PacketMultipartClientPartAddition, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartClientPartAddition message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartA_Base multipart = (EntityMultipartA_Base) getMultipart(message, ctx);
					PackPart packPart = multipart.getPackDefForLocation(message.offsetX, message.offsetY, message.offsetZ);
					String partName = ((AItemPart) message.partStack.getItem()).partName;
					try{
						Class<? extends APart> partClass = PackParserSystem.getPartPartClass(partName);
						Constructor<? extends APart> construct = partClass.getConstructor(EntityMultipartD_Moving.class, PackPart.class, String.class, NBTTagCompound.class);
						APart newPart = construct.newInstance((EntityMultipartD_Moving) multipart, packPart, partName, message.partStack.hasTagCompound() ? message.partStack.getTagCompound() : new NBTTagCompound());
						multipart.addPart(newPart, false);
					}catch(Exception e){
						MTS.MTSLog.error("ERROR SPAWING PART ON CLIENT!");
						MTS.MTSLog.error(e.getMessage());
					}
				}
			});
			return null;
		}
	}

}
