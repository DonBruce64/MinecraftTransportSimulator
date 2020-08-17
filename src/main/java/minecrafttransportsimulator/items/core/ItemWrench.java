package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import mcinterface.BuilderGUI;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.guis.instances.GUIInstruments;
import minecrafttransportsimulator.guis.instances.GUIVehicleEditor;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		//If the player isn't the owner of the vehicle, they can't interact with it.
		if(!ownerState.equals(PlayerOwnerState.USER)){
			if(rightClick){
				if(vehicle.world.isClient()){
					if(vehicle.equals(player.getEntityRiding()) && ConfigSystem.configObject.client.devMode.value){
						BuilderGUI.openGUI(new GUIVehicleEditor(vehicle));
					}else{
						BuilderGUI.openGUI(new GUIInstruments(vehicle, player));
					}
				}else{
					return CallbackType.PLAYER;
				}
			}else if(!vehicle.world.isClient()){
				if(part != null && !player.isSneaking()){
					//Player can remove part.  Spawn item in the world and remove part.
					//Make sure to remove the part before spawning the item.  Some parts
					//care about this order and won't spawn items unless they've been removed.
					vehicle.removePart(part, null, false);
					Item droppedItem = part.getItem();
					if(droppedItem != null){
						vehicle.world.spawnItemStack(new ItemStack(droppedItem), part.getData(), part.worldPos);
					}
				}else if(player.isSneaking()){
					//Attacker is a sneaking player with a wrench.
					//Remove this vehicle if possible.
					if((!ConfigSystem.configObject.general.opPickupVehiclesOnly.value || ownerState.equals(PlayerOwnerState.ADMIN)) && (!ConfigSystem.configObject.general.creativePickupVehiclesOnly.value || player.isCreative())){
						ItemStack vehicleStack = new ItemStack(MTSRegistry.packItemMap.get(vehicle.definition.packID).get(vehicle.definition.systemName));
						WrapperNBT vehicleData = new WrapperNBT();
						vehicle.save(vehicleData);
						vehicle.world.spawnItemStack(vehicleStack, vehicleData, vehicle.position);
						vehicle.isValid = false;
					}
				}
			}
		}else{
			player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehicleowned"));
		}
		return CallbackType.NONE;
	}
}
