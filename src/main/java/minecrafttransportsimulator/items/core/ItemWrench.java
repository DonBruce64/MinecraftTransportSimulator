package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import mcinterface.BuilderGUI;
import mcinterface.InterfaceNetwork;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketVehicleWrenchGUI;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
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
		tooltipLines.add(BuilderGUI.translate("info.item.wrench.use"));
		tooltipLines.add(BuilderGUI.translate("info.item.wrench.useblock"));
		tooltipLines.add(BuilderGUI.translate("info.item.wrench.attack"));
		tooltipLines.add(BuilderGUI.translate("info.item.wrench.sneakattack"));
		if(ConfigSystem.configObject.client.devMode.value){
			tooltipLines.add("Use on a vehicle while seated in it to activate the DevMode editor.");
		}
	}
	
	@Override
	public void doVehicleInteraction(ItemStack stack, EntityVehicleF_Physics vehicle, APart part, EntityPlayerMP player, PlayerOwnerState ownerState, boolean rightClick){
		//If the player isn't the owner of the vehicle, they can't interact with it.
		if(!ownerState.equals(PlayerOwnerState.USER)){
			if(rightClick){
				InterfaceNetwork.sendToPlayer(new PacketVehicleWrenchGUI(vehicle), player);
			}else{
				if(part != null && !player.isSneaking()){
					//Player can remove part.  Spawn item in the world and remove part.
					//Make sure to remove the part before spawning the item.  Some parts
					//care about this order and won't spawn items unless they've been removed.
					vehicle.removePart(part, false);
					Item droppedItem = part.getItem();
					if(droppedItem != null){
						ItemStack droppedStack = new ItemStack(droppedItem);
						droppedStack.setTagCompound(part.getData());
						vehicle.world.spawnEntity(new EntityItem(vehicle.world, part.worldPos.x, part.worldPos.y, part.worldPos.z, droppedStack));
					}
				}else if(player.isSneaking()){
					//Attacker is a sneaking player with a wrench.
					//Remove this vehicle if possible.
					if((!ConfigSystem.configObject.general.opPickupVehiclesOnly.value || ownerState.equals(PlayerOwnerState.ADMIN)) && (!ConfigSystem.configObject.general.creativePickupVehiclesOnly.value || player.capabilities.isCreativeMode)){
						ItemStack vehicleStack = new ItemStack(MTSRegistry.packItemMap.get(vehicle.definition.packID).get(vehicle.definition.systemName));
						NBTTagCompound stackTag = vehicle.writeToNBT(new NBTTagCompound());
						vehicleStack.setTagCompound(stackTag);
						vehicle.world.spawnEntity(new EntityItem(vehicle.world, vehicle.posX, vehicle.posY, vehicle.posZ, vehicleStack));
						vehicle.setDead();
					}
				}
			}
		}else{
			InterfaceNetwork.sendToPlayer(new PacketPlayerChatMessage("interact.failure.vehicleowned"), player);
		}
	}
}
