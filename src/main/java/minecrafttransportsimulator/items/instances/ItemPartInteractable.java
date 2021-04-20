package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import net.minecraft.item.ItemStack;

public class ItemPartInteractable extends AItemPart implements IItemVehicleInteractable{
	
	public ItemPartInteractable(JSONPart definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}
	
	@Override
	public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, String subNameToPlaceOn, boolean checkMinMax){
		return super.isPartValidForPackDef(placementDefinition, subNameToPlaceOn, checkMinMax) && (!checkMinMax || (placementDefinition.minValue <= definition.interactable.inventoryUnits && placementDefinition.maxValue >= definition.interactable.inventoryUnits));
	}
	
	@Override
	public PartInteractable createPart(AEntityE_Multipart<?> entity, JSONPartDefinition packVehicleDef, WrapperNBT partData, APart parentPart){
		return new PartInteractable(entity, packVehicleDef, validateData(partData), parentPart);
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		switch(definition.interactable.interactionType){
			case CRATE : {
				tooltipLines.add(InterfaceCore.translate("info.item.interactable.capacity") + definition.interactable.inventoryUnits*9);
				break;
			}
			case BARREL : {
				tooltipLines.add(InterfaceCore.translate("info.item.interactable.capacity") + definition.interactable.inventoryUnits*10000 + "mb");
				break;
			}
			case JERRYCAN : {
				tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.fill"));
				tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.drain"));
				String jerrycanFluid = data.getString("jerrycanFluid");
				if(jerrycanFluid.isEmpty()){
					tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.empty"));
				}else{
					tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.contains") + InterfaceCore.getFluidName(jerrycanFluid));
				}
				break;
			}
			default : {
				//Don't add tooltips for other things.
			}
		}
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(definition.interactable.interactionType.equals(InteractableComponentType.JERRYCAN)){
			if(!vehicle.world.isClient()){
				if(rightClick){
					ItemStack stack = player.getHeldStack();
					WrapperNBT data = new WrapperNBT(stack);
					String jerrrycanFluid = data.getString("jerrycanFluid");
					
					//If we clicked a tank on the vehicle, attempt to pull from it rather than fill the vehicle.
					if(part instanceof PartInteractable){
						EntityFluidTank tank = ((PartInteractable) part).tank;
						if(tank != null){
							if(jerrrycanFluid.isEmpty()){
								if(tank.getFluidLevel() >= 1000){
									data.setString("jerrycanFluid", tank.getFluid());
									stack.setTagCompound(data.tag);
									tank.drain(tank.getFluid(), 1000, true);
								}
							}
						}
					}else if(!jerrrycanFluid.isEmpty()){
						if(vehicle.fuelTank.getFluid().isEmpty() || vehicle.fuelTank.getFluid().equals(jerrrycanFluid)){
							if(vehicle.fuelTank.getFluidLevel() + 1000 > vehicle.fuelTank.getMaxLevel()){
								player.sendPacket(new PacketPlayerChatMessage(player, "interact.jerrycan.toofull"));
							}else{
								vehicle.fuelTank.fill(jerrrycanFluid, 1000, true);
								data.setString("jerrycanFluid", "");
								stack.setTagCompound(data.tag);
								player.sendPacket(new PacketPlayerChatMessage(player, "interact.jerrycan.success"));
							}
						}else{
							player.sendPacket(new PacketPlayerChatMessage(player, "interact.jerrycan.wrongtype"));
						}
					}else{
						player.sendPacket(new PacketPlayerChatMessage(player, "interact.jerrycan.empty"));
					}
				}
			}
			return CallbackType.NONE;
		}
		return CallbackType.SKIP;
	}
	
	public static final AItemPartCreator CREATOR = new AItemPartCreator(){
		@Override
		public boolean isCreatorValid(JSONPart definition){
			return definition.generic.type.startsWith("interactable");
		}
		@Override
		public ItemPartInteractable createItem(JSONPart definition, String subName, String sourcePackID){
			return new ItemPartInteractable(definition, subName, sourcePackID);
		}
	};
}
