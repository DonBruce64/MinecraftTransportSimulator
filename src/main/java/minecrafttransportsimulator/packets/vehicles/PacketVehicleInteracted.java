package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.core.ItemKey;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleInteracted extends APacketVehiclePlayer{

	public PacketVehicleInteracted(){}
	
	public PacketVehicleInteracted(EntityVehicleB_Existing vehicle, EntityPlayer player){
		super(vehicle, player);
	}

	public static class Handler implements IMessageHandler<PacketVehicleInteracted, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleInteracted message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleB_Existing vehicle = (EntityVehicleB_Existing) getVehicle(message, ctx);
					EntityPlayer player = getPlayer(message, ctx);
					
					if(vehicle != null && player != null && !vehicle.isDead){
						//If we clicked a part, try to interact with it.
						//If we didn't interact with a part, check other interactions.
						//All interactions are done on the server, except GUI openings.
						APart hitPart = vehicle.getHitPart(player);
						ItemStack heldStack = player.getHeldItemMainhand();
						if(hitPart != null && hitPart.interactPart(player)){
							return;
						}else if(heldStack.getItem().equals(MTSRegistry.key)){
							ItemKey key = (ItemKey) heldStack.getItem();
							//Sneaking changes ownership, regular use changes the lock state.
							if(player.isSneaking()){
								if(vehicle.ownerName.isEmpty()){
									vehicle.ownerName = player.getUUID(player.getGameProfile()).toString();
									MTS.MTSNet.sendTo(new PacketChat("interact.key.info.own"), (EntityPlayerMP) player);
								}else{
									boolean isPlayerOP = player.getServer() == null || player.getServer().isSinglePlayer() || player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null;
									if(player.getUUID(player.getGameProfile()).toString().equals(vehicle.ownerName) || isPlayerOP){
										vehicle.ownerName = "";
										MTS.MTSNet.sendTo(new PacketChat("interact.key.info.unown"), (EntityPlayerMP) player);
									}else{
										MTS.MTSNet.sendTo(new PacketChat("interact.key.failure.alreadyowned"), (EntityPlayerMP) player);
									}
								}
							}else{
								String vehicleUUID = heldStack.hasTagCompound() ? heldStack.getTagCompound().getString("vehicle") : "";
								if(vehicleUUID.isEmpty()){
									if(!vehicle.ownerName.isEmpty()){
										if(!player.getUUID(player.getGameProfile()).toString().equals(vehicle.ownerName)){
											MTS.MTSNet.sendTo(new PacketChat("interact.key.failure.notowner"), (EntityPlayerMP) player);
										}
									}
									NBTTagCompound tag = new NBTTagCompound();
									tag.setString("vehicle", vehicle.getUniqueID().toString());
									heldStack.setTagCompound(tag);
									vehicle.locked = true;
									MTS.MTSNet.sendTo(new PacketChat("interact.key.info.lock"), (EntityPlayerMP) player);
								}else if(!vehicleUUID.equals(vehicle.getUniqueID().toString())){
									MTS.MTSNet.sendTo(new PacketChat("interact.key.failure.wrongkey"), (EntityPlayerMP) player);
								}else{
									if(vehicle.locked){
										vehicle.locked = false;
										MTS.MTSNet.sendTo(new PacketChat("interact.key.info.unlock"), (EntityPlayerMP) player);
									}else{
										vehicle.locked = true;
										MTS.MTSNet.sendTo(new PacketChat("interact.key.info.lock"), (EntityPlayerMP) player);
									}
								}
							}
							MTS.MTSNet.sendToAll(new PacketVehicleKey(vehicle));
						}else if(Items.NAME_TAG.equals(player.getHeldItemMainhand().getItem())){
							vehicle.displayText = heldStack.getDisplayName().length() > vehicle.definition.rendering.displayTextMaxLength ? heldStack.getDisplayName().substring(0, vehicle.definition.rendering.displayTextMaxLength - 1) : heldStack.getDisplayName();
							MTS.MTSNet.sendToAll(new PacketVehicleNameTag(vehicle));
						}else if(MTSRegistry.jerrycan.equals(player.getHeldItemMainhand().getItem())){
							if(heldStack.hasTagCompound() && heldStack.getTagCompound().getBoolean("isFull")){
								EntityVehicleE_Powered poweredVehicle = (EntityVehicleE_Powered) vehicle;
								if(poweredVehicle.fluidName.isEmpty() || poweredVehicle.fluidName.equals(heldStack.getTagCompound().getString("fluidName"))){
									if(poweredVehicle.fuel + 1000 > poweredVehicle.definition.motorized.fuelCapacity){
										MTS.MTSNet.sendTo(new PacketChat("interact.jerrycan.toofull"), (EntityPlayerMP) player);
									}else{
										poweredVehicle.fluidName = heldStack.getTagCompound().getString("fluidName");
										poweredVehicle.fuel += 1000;
										heldStack.setTagCompound(null);
										MTS.MTSNet.sendTo(new PacketChat("interact.jerrycan.success"), (EntityPlayerMP) player);
										MTS.MTSNet.sendToAll(new PacketVehicleJerrycan(vehicle, poweredVehicle.fluidName));
									}
								}else{
									MTS.MTSNet.sendTo(new PacketChat("interact.jerrycan.wrongtype"), (EntityPlayerMP) player);
								}
							}else{
								MTS.MTSNet.sendTo(new PacketChat("interact.jerrycan.empty"), (EntityPlayerMP) player);
							}
						}
					}
				}
			});
			return null;
		}
	}
}
