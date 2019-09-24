package minecrafttransportsimulator.packets.vehicles;

import java.lang.reflect.Constructor;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleServerPartAddition extends APacketVehiclePart{
	private int player;

	public PacketVehicleServerPartAddition(){}
	
	public PacketVehicleServerPartAddition(EntityVehicleA_Base vehicle, double offsetX, double offsetY, double offsetZ, EntityPlayer player){
		super(vehicle, offsetX, offsetY, offsetZ);
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

	public static class Handler implements IMessageHandler<PacketVehicleServerPartAddition, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleServerPartAddition message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					//Check to make sure we can actually add this part before we do so.
					EntityVehicleA_Base vehicle = (EntityVehicleA_Base) getVehicle(message, ctx);
					EntityPlayer player;
					if(ctx.side.isServer()){
						player = (EntityPlayer) ctx.getServerHandler().player.world.getEntityByID(message.player);
					}else{
						player = (EntityPlayer) Minecraft.getMinecraft().world.getEntityByID(message.player);
					}
					
					ItemStack heldStack = player.getHeldItemMainhand();
					if(heldStack != null){
						if(heldStack.getItem() instanceof AItemPart){
							//Player is holding a valid part.  Now check if part goes to this vehicle.
							AItemPart partItem = (AItemPart) heldStack.getItem();
							PackPart packPart = vehicle.getPackDefForLocation(message.offsetX, message.offsetY, message.offsetZ);
							PackPartObject itemPack = PackParserSystem.getPartPack(partItem.partName);
							if(packPart.types.contains(itemPack.general.type)){
								//This part does go to this vehicle.  Now check if one is already there.
								Vec3d partOffset = new Vec3d(message.offsetX, message.offsetY, message.offsetZ);
        						for(APart part : vehicle.getVehicleParts()){
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
									Constructor<? extends APart> construct = partClass.getConstructor(EntityVehicleE_Powered.class, PackPart.class, String.class, NBTTagCompound.class);
									APart newPart = construct.newInstance((EntityVehicleE_Powered) vehicle, packPart, partItem.partName, heldStack.hasTagCompound() ? heldStack.getTagCompound() : new NBTTagCompound());
									vehicle.addPart(newPart, false);
									MTS.MTSNet.sendToAll(new PacketVehicleClientPartAddition(vehicle, message.offsetX, message.offsetY, message.offsetZ, heldStack));
									if(!player.capabilities.isCreativeMode){
										player.inventory.clearMatchingItems(partItem, heldStack.getItemDamage(), 1, heldStack.getTagCompound());
									}
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
