package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleKey;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemKey extends Item implements IItemVehicleInteractable{
	
	public ItemKey(){
		super();
		setFull3D();
		this.setMaxStackSize(1);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		for(byte i=1; i<=5; ++i){
			tooltipLines.add(I18n.format("info.item.key.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public void doVehicleInteraction(ItemStack stack, EntityVehicleF_Physics vehicle, APart part, EntityPlayerMP player, PlayerOwnerState ownerState, boolean rightClick){
		if(rightClick){
			if(player.isSneaking()){
				//Try to change ownership of the vehicle.
				if(vehicle.ownerName.isEmpty()){
					vehicle.ownerName = EntityPlayer.getUUID(player.getGameProfile()).toString();
					MTS.MTSNet.sendTo(new PacketChat("interact.key.info.own"), player);
				}else{
					if(!ownerState.equals(PlayerOwnerState.USER)){
						vehicle.ownerName = "";
						MTS.MTSNet.sendTo(new PacketChat("interact.key.info.unown"), player);
					}else{
						MTS.MTSNet.sendTo(new PacketChat("interact.key.failure.alreadyowned"), player);
					}
				}
			}else{
				//Try to lock the vehicle.
				//First check to see if we need to set this key's vehicle.
				String keyVehicleUUID = stack.hasTagCompound() ? stack.getTagCompound().getString("vehicle") : "";
				String vehicleUUID = vehicle.getUniqueID().toString();
				if(keyVehicleUUID.isEmpty()){
					//Check if we are the owner before making this a valid key.
					if(!vehicle.ownerName.isEmpty() && ownerState.equals(PlayerOwnerState.USER)){
						MTS.MTSNet.sendTo(new PacketChat("interact.key.failure.notowner"), player);
						return;
					}
					
					keyVehicleUUID = vehicleUUID;
					NBTTagCompound tag = new NBTTagCompound();
					tag.setString("vehicle", keyVehicleUUID);
					stack.setTagCompound(tag);
				}
				
				//Try to lock or unlock this vehicle.
				if(!keyVehicleUUID.equals(vehicleUUID)){
					MTS.MTSNet.sendTo(new PacketChat("interact.key.failure.wrongkey"), player);
				}else{
					if(vehicle.locked){
						vehicle.locked = false;
						MTS.MTSNet.sendTo(new PacketChat("interact.key.info.unlock"), player);
						//If we aren't in this vehicle, and we clicked a seat, start riding the vehicle.
						if(part instanceof PartSeat && player.getRidingEntity() == null){
							part.interactPart(player);
						}
					}else{
						vehicle.locked = true;
						MTS.MTSNet.sendTo(new PacketChat("interact.key.info.lock"), player);
					}
					MTS.MTSNet.sendToAll(new PacketVehicleKey(vehicle));
				}
			}
		}
	}
}
