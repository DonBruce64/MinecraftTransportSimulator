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
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartClientPartAddition extends APacketMultipart{
	private byte partIndex;
	private ItemStack partStack;

	public PacketMultipartClientPartAddition(){}
	
	public PacketMultipartClientPartAddition(EntityMultipartA_Base multipart, byte partIndex, ItemStack partStack){
		super(multipart);
		this.partIndex = partIndex;
		this.partStack = partStack;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.partIndex=buf.readByte();
		this.partStack=ByteBufUtils.readItemStack(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeByte(this.partIndex);
		ByteBufUtils.writeItemStack(buf, partStack);
	}

	public static class Handler implements IMessageHandler<PacketMultipartClientPartAddition, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartClientPartAddition message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartA_Base multipart = (EntityMultipartA_Base) getMultipartFromMessage(message, ctx);
					PackPart packPart = multipart.pack.parts.get(message.partIndex);
					String partName = ((AItemPart) message.partStack.getItem()).partName;
					Vec3d partOffset = new Vec3d(packPart.pos[0], packPart.pos[1], packPart.pos[2]);
					try{
						Class<? extends APart> partClass = PackParserSystem.getPartPartClass(partName);
						Constructor<? extends APart> construct = partClass.getConstructor(EntityMultipartD_Moving.class, Vec3d.class, boolean.class, boolean.class, String.class, NBTTagCompound.class);
						APart newPart = construct.newInstance((EntityMultipartD_Moving) multipart, partOffset, packPart.isController, packPart.turnsWithSteer, partName, message.partStack.getTagCompound());
						multipart.addPart(newPart);
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
