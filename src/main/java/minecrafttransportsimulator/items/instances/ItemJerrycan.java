package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;
import net.minecraft.item.ItemStack;

public class ItemJerrycan extends AItemBase implements IItemVehicleInteractable{
		
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.fill"));
		tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.drain"));
		if(data.getBoolean("isFull")){
			tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.contains") + InterfaceCore.getFluidName(data.getString("fluidName")));
		}else{
			tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.empty"));
		}
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
	
	@Override
	public boolean canBeStacked(){
		return false;
	}
}
