package minecrafttransportsimulator.packets.vehicles;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.items.packs.parts.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleClientPartAddition extends APacketVehiclePart{
	private ItemStack partStack;

	public PacketVehicleClientPartAddition(){}
	
	public PacketVehicleClientPartAddition(EntityVehicleA_Base vehicle, double offsetX, double offsetY, double offsetZ, ItemStack partStack){
		super(vehicle, offsetX, offsetY, offsetZ);
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

	public static class Handler implements IMessageHandler<PacketVehicleClientPartAddition, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleClientPartAddition message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleA_Base vehicle = (EntityVehicleA_Base) getVehicle(message, ctx);
					if(vehicle != null){
						VehiclePart packPart = vehicle.getPackDefForLocation(message.offsetX, message.offsetY, message.offsetZ);
						vehicle.addPart(PackParserSystem.createPart((EntityVehicleE_Powered) vehicle, packPart, ((AItemPart) message.partStack.getItem()).definition, message.partStack.hasTagCompound() ? message.partStack.getTagCompound() : new NBTTagCompound()), false);
					}
				}
			});
			return null;
		}
	}

}
