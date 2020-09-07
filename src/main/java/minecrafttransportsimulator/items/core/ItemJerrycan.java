package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import mcinterface.BuilderGUI;
import mcinterface.WrapperPlayer;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemJerrycan extends Item implements IItemVehicleInteractable{
	public ItemJerrycan(){
		super();
		setFull3D();
		setMaxStackSize(1);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		tooltipLines.add(BuilderGUI.translate("info.item.jerrycan.fill"));
		tooltipLines.add(BuilderGUI.translate("info.item.jerrycan.drain"));
		if(stack.hasTagCompound() && stack.getTagCompound().getBoolean("isFull")){
			tooltipLines.add(BuilderGUI.translate("info.item.jerrycan.contains") + new FluidStack(FluidRegistry.getFluid(stack.getTagCompound().getString("fluidName")), 1000).getLocalizedName());
		}else{
			tooltipLines.add(BuilderGUI.translate("info.item.jerrycan.empty"));
		}
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(!vehicle.world.isClient()){
			if(rightClick){
				//If we clicked a tank on the vehicle, attempt to pull from it rather than fill the vehicle.
				if(part instanceof PartInteractable){
					FluidTank tank = ((PartInteractable) part).tank;
					if(tank != null && tank.interactWith(player)){
						return CallbackType.NONE;
					}
				}
				ItemStack stack = player.getHeldStack();
				if(stack.hasTagCompound() && stack.getTagCompound().getBoolean("isFull")){
					if(vehicle.fuelTank.getFluid().isEmpty() || vehicle.fuelTank.getFluid().equals(stack.getTagCompound().getString("fluidName"))){
						if(vehicle.fuelTank.getFluidLevel() + 1000 > vehicle.fuelTank.getMaxLevel()){
							player.sendPacket(new PacketPlayerChatMessage("interact.jerrycan.toofull"));
						}else{
							vehicle.fuelTank.fill(stack.getTagCompound().getString("fluidName"), 1000, true);
							stack.setTagCompound(null);
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
}
