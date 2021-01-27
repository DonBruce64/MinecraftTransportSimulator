package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUIBooklet;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemFood;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;
import net.minecraft.item.ItemStack;

public class ItemItem extends AItemPack<JSONItem> implements IItemFood, IItemVehicleInteractable{
	/*Current page of this item, if it's a booklet.  Kept here locally as only one item class is constructed for each booklet definition.*/
	public int pageNumber;
	
	public ItemItem(JSONItem definition){
		super(definition, null);
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		if(ItemComponentType.JERRYCAN.equals(definition.general.type)){
			tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.fill"));
			tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.drain"));
			if(data.getBoolean("isFull")){
				tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.contains") + InterfaceCore.getFluidName(data.getString("fluidName")));
			}else{
				tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.empty"));
			}
		}
	}
	
	@Override
	public boolean onUsed(WrapperWorld world, WrapperPlayer player){
		if(world.isClient() && ItemComponentType.BOOKLET.equals(definition.general.type)){
			InterfaceGUI.openGUI(new GUIBooklet(this));
		}
        return true;
    }

	@Override
	public int getTimeToEat(){
		return ItemComponentType.FOOD.equals(definition.general.type) ? definition.food.timeToEat : 0;
	}
	
	@Override
	public boolean isDrink(){
		return definition.food.isDrink;
	}

	@Override
	public int getHungerAmount(){
		return definition.food.hungerAmount;
	}

	@Override
	public float getSaturationAmount(){
		return definition.food.saturationAmount;
	}

	@Override
	public List<JSONPotionEffect> getEffects(){
		return definition.food.effects;
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(!vehicle.world.isClient()){
			if(rightClick){
				ItemStack stack = player.getHeldStack();
				WrapperNBT data = new WrapperNBT(stack);
				
				//If we clicked a tank on the vehicle, attempt to pull from it rather than fill the vehicle.
				if(part instanceof PartInteractable){
					FluidTank tank = ((PartInteractable) part).tank;
					if(tank != null){
						if(!data.getBoolean("isFull")){
							if(tank.getFluidLevel() >= 1000){
								data.setBoolean("isFull", true);
								data.setString("fluidName", tank.getFluid());
								stack.setTagCompound(data.tag);
								tank.drain(tank.getFluid(), 1000, true);
							}
						}
					}
				}else if(data.getBoolean("isFull")){
					if(vehicle.fuelTank.getFluid().isEmpty() || vehicle.fuelTank.getFluid().equals(data.getString("fluidName"))){
						if(vehicle.fuelTank.getFluidLevel() + 1000 > vehicle.fuelTank.getMaxLevel()){
							player.sendPacket(new PacketPlayerChatMessage("interact.jerrycan.toofull"));
						}else{
							vehicle.fuelTank.fill(data.getString("fluidName"), 1000, true);
							data.setBoolean("isFull", false);
							data.setString("fluidName", "");
							stack.setTagCompound(data.tag);
							player.sendPacket(new PacketPlayerChatMessage("interact.jerrycan.success"));
						}
					}else{
						player.sendPacket(new PacketPlayerChatMessage("interact.jerrycan.wrongtype"));
					}
				}else{
					player.sendPacket(new PacketPlayerChatMessage("interact.jerrycan.empty"));
				}
			}
		}
		return CallbackType.NONE;
	}
	
	public static enum ItemComponentType{
		@JSONDescription("Creates an item with no functionality.")
		NONE,
		@JSONDescription("Creates a booklet, which is a book-like item.")
		BOOKLET,
		@JSONDescription("Creates an item that can be eaten.")
		FOOD,
		@JSONDescription("Creates an item that functions as a jerrycan.")
		JERRYCAN;
	}
}
