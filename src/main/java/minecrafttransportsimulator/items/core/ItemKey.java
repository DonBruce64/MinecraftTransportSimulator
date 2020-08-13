package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import mcinterface.BuilderGUI;
import mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.minecraft.client.util.ITooltipFlag;
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
			tooltipLines.add(BuilderGUI.translate("info.item.key.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(!vehicle.world.isClient()){
			if(rightClick){
				if(player.isSneaking()){
					//Try to change ownership of the vehicle.
					if(vehicle.ownerUUID.isEmpty()){
						vehicle.ownerUUID = player.getUUID();
						player.sendPacket(new PacketPlayerChatMessage("interact.key.info.own"));
					}else{
						if(!ownerState.equals(PlayerOwnerState.USER)){
							vehicle.ownerUUID = "";
							player.sendPacket(new PacketPlayerChatMessage("interact.key.info.unown"));
						}else{
							player.sendPacket(new PacketPlayerChatMessage("interact.key.failure.alreadyowned"));
						}
					}
				}else{
					//Try to lock the vehicle.
					//First check to see if we need to set this key's vehicle.
					ItemStack stack = player.getHeldStack();
					String keyVehicleUUID = stack.hasTagCompound() ? stack.getTagCompound().getString("vehicle") : "";
					if(keyVehicleUUID.isEmpty()){
						//Check if we are the owner before making this a valid key.
						if(!vehicle.ownerUUID.isEmpty() && ownerState.equals(PlayerOwnerState.USER)){
							player.sendPacket(new PacketPlayerChatMessage("interact.key.failure.notowner"));
							return CallbackType.NONE;
						}
						
						keyVehicleUUID = vehicle.uniqueUUID;
						NBTTagCompound tag = new NBTTagCompound();
						tag.setString("vehicle", keyVehicleUUID);
						stack.setTagCompound(tag);
					}
					
					//Try to lock or unlock this vehicle.
					//If we succeed, send callback to clients to change locked state.
					if(!keyVehicleUUID.equals(vehicle.uniqueUUID)){
						player.sendPacket(new PacketPlayerChatMessage("interact.key.failure.wrongkey"));
					}else{
						if(vehicle.locked){
							vehicle.locked = false;
							player.sendPacket(new PacketPlayerChatMessage("interact.key.info.unlock"));
							//If we aren't in this vehicle, and we clicked a seat, start riding the vehicle.
							if(part instanceof PartSeat && player.getEntityRiding() == null){
								part.interact(player);
							}
						}else{
							vehicle.locked = true;
							player.sendPacket(new PacketPlayerChatMessage("interact.key.info.lock"));
						}
						return CallbackType.ALL;
					}
				}
			}
		}else{
			vehicle.locked = !vehicle.locked;
		}
		return CallbackType.NONE;
	}
}
