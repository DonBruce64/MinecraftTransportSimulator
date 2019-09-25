package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleAttacked extends APacketVehiclePlayer{

	public PacketVehicleAttacked(){}
	
	public PacketVehicleAttacked(EntityVehicleB_Existing vehicle, EntityPlayer player){
		super(vehicle, player);
	}

	public static class Handler implements IMessageHandler<PacketVehicleAttacked, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleAttacked message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleB_Existing vehicle = (EntityVehicleB_Existing) getVehicle(message, ctx);
					EntityPlayer player = getPlayer(message, ctx);
					
					if(vehicle != null && player != null){
						if(player.getHeldItemMainhand() != null && MTSRegistry.wrench.equals(player.getHeldItemMainhand().getItem())){
							if(player.isSneaking()){
								//Attacker is a sneaking player with a wrench.
								//Remove this entity if possible.
								boolean isPlayerOP = player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
								if(vehicle.ownerName.isEmpty() || player.getUUID(player.getGameProfile()).toString().equals(vehicle.ownerName) || isPlayerOP){
									ItemStack stack = new ItemStack(MTSRegistry.vehicleItemMap.get(vehicle.vehicleName));
									NBTTagCompound stackTag = vehicle.writeToNBT(new NBTTagCompound());
									stack.setTagCompound(stackTag);
									vehicle.world.spawnEntity(new EntityItem(vehicle.world, vehicle.posX, vehicle.posY, vehicle.posZ, stack));
									vehicle.setDead();
								}else{
									MTS.MTSNet.sendTo(new PacketChat("interact.failure.vehicleowned"), (EntityPlayerMP) player);
								}
								return;
							}else{
								//Attacker is a player swinging a wrench.
								//If they hit a part, remove it if pemitted.
								APart hitPart = vehicle.getHitPart(player);
								if(hitPart != null){
									boolean isPlayerOP = player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
									if(vehicle.ownerName.isEmpty() || player.getUUID(player.getGameProfile()).toString().equals(vehicle.ownerName) || isPlayerOP){
										//Player can remove part.  Spawn item in the world and remove part.
										//Make sure to remove the part before spawning the item.  Some parts
										//care about this order and won't spawn items unless they've been removed.
										vehicle.removePart(hitPart, false);
										Item droppedItem = hitPart.getItemForPart();
										if(droppedItem != null){
											ItemStack droppedStack = new ItemStack(droppedItem);
											droppedStack.setTagCompound(hitPart.getPartNBTTag());
											vehicle.world.spawnEntity(new EntityItem(vehicle.world, hitPart.partPos.x, hitPart.partPos.y, hitPart.partPos.z, droppedStack));
										}
									}else{
										MTS.MTSNet.sendTo(new PacketChat("interact.failure.vehicleowned"), (EntityPlayerMP) player);
									}
								}
							}
						}else{
							vehicle.attackEntityFrom(DamageSource.causePlayerDamage(player), 1.0F);
						}
					}
				}
			});
			return null;
		}
	}
}
