package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleWrench;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemWrench extends Item implements IItemVehicleInteractable{
	public ItemWrench(){
		super();
		setFull3D();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		tooltipLines.add(I18n.format("info.item.wrench.use"));
		tooltipLines.add(I18n.format("info.item.wrench.attack"));
		tooltipLines.add(I18n.format("info.item.wrench.sneakattack"));
		if(ConfigSystem.configObject.client.devMode.value){
			tooltipLines.add("Use while seated in a vehicle to activate the DevMode editor.");
		}
	}
	
	@Override
	public void doVehicleInteraction(ItemStack stack, EntityVehicleE_Powered vehicle, APart part, EntityPlayerMP player, PlayerOwnerState ownerState, boolean rightClick){
		//If the player isn't the owner of the vehicle, they can't interact with it.
		if(!ownerState.equals(PlayerOwnerState.USER)){
			if(rightClick){
				MTS.MTSNet.sendTo(new PacketVehicleWrench(vehicle, player), player);
			}else{
				if(part != null && !player.isSneaking()){
					//Player can remove part.  Spawn item in the world and remove part.
					//Make sure to remove the part before spawning the item.  Some parts
					//care about this order and won't spawn items unless they've been removed.
					vehicle.removePart(part, false);
					Item droppedItem = part.getItemForPart();
					if(droppedItem != null){
						ItemStack droppedStack = new ItemStack(droppedItem);
						droppedStack.setTagCompound(part.getPartNBTTag());
						vehicle.world.spawnEntity(new EntityItem(vehicle.world, part.partPos.x, part.partPos.y, part.partPos.z, droppedStack));
					}
				}else if(player.isSneaking()){
					//Attacker is a sneaking player with a wrench.
					//Remove this vehicle if possible.
					if(!ConfigSystem.configObject.general.opPickupVehiclesOnly.value || ownerState.equals(PlayerOwnerState.ADMIN)){
						ItemStack vehicleStack = new ItemStack(MTSRegistry.packItemMap.get(vehicle.definition.packID).get(vehicle.definition.systemName));
						NBTTagCompound stackTag = vehicle.writeToNBT(new NBTTagCompound());
						vehicleStack.setTagCompound(stackTag);
						vehicle.world.spawnEntity(new EntityItem(vehicle.world, vehicle.posX, vehicle.posY, vehicle.posZ, vehicleStack));
						vehicle.setDead();
					}
				}
			}
		}else{
			MTS.MTSNet.sendTo(new PacketChat("interact.failure.vehicleowned"), player);
		}
	}
}
