package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.core.ItemKey;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleKey extends APacketVehiclePlayer{
	
	public PacketVehicleKey(){}
	
	public PacketVehicleKey(EntityVehicleB_Existing vehicle, EntityPlayer player){
		super(vehicle, player);
	}
	
	public static class Handler implements IMessageHandler<PacketVehicleKey, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleKey message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleB_Existing vehicle = (EntityVehicleB_Existing) getVehicle(message, ctx);
					EntityPlayer player = getPlayer(message, ctx);
					
					String messageString = "";
					ItemStack heldStack = player.getHeldItemMainhand();
					if(heldStack != null){
						if(MTSRegistry.key.equals(heldStack.getItem())){
							ItemKey key = (ItemKey) heldStack.getItem();
							if(player.isSneaking()){
								//If player is sneaking with a key we need to change ownership.
								messageString = changeOwner(heldStack, vehicle, player);
							}else{
								//Not sneaking, do locking code.
								messageString = changeLock(heldStack, vehicle, player);
							}
							if(ctx.side.isServer()){
								MTS.MTSNet.sendToAll(message);
								if(!messageString.isEmpty()){ 
									MTS.MTSNet.sendTo(new PacketChat(messageString), (EntityPlayerMP) player);
								}
							}
						}
					}
				}
			});
			return null;
		}
		
		private static String changeOwner(ItemStack stack, EntityVehicleB_Existing vehicle, EntityPlayer player){
			if(vehicle.ownerName.isEmpty()){
				//No owner, take ownership.
				vehicle.ownerName = player.getUUID(player.getGameProfile()).toString();
				return "interact.key.info.own";
			}else{
				//Already owned, check to see if we can disown.
				boolean isPlayerOP = player.getServer() == null || player.getServer().isSinglePlayer() || player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null;
				if(player.getUUID(player.getGameProfile()).toString().equals(vehicle.ownerName) || isPlayerOP){
					vehicle.ownerName = "";
					return "interact.key.info.unown";	
				}else{
					return "interact.key.failure.alreadyowned";
				}
			}
		}
		
		private static String changeLock(ItemStack stack, EntityVehicleB_Existing vehicle, EntityPlayer player){
			String vehicleUUID = stack.hasTagCompound() ? stack.getTagCompound().getString("vehicle") : "";
			if(vehicleUUID.isEmpty()){
				if(!vehicle.ownerName.isEmpty()){
					if(!player.getUUID(player.getGameProfile()).toString().equals(vehicle.ownerName)){
						return "interact.key.failure.notowner";
					}
				}
				NBTTagCompound tag = new NBTTagCompound();
				tag.setString("vehicle", vehicle.getUniqueID().toString());
				stack.setTagCompound(tag);
				
				vehicle.locked = true;
				return "interact.key.info.lock";
			}else if(!vehicleUUID.equals(vehicle.getUniqueID().toString())){
				return "interact.key.failure.wrongkey";
			}else{
				if(vehicle.locked){
					vehicle.locked = false;
					return "interact.key.info.unlock";
				}else{
					vehicle.locked = true;
					return "interact.key.info.lock";
				}
			}
		}
	}
}
