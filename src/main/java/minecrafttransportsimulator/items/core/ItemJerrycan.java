package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import mcinterface.BuilderGUI;
import mcinterface.WrapperPlayer;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleJerrycan;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
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
	public void doVehicleInteraction(ItemStack stack, EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(rightClick){
			if(stack.hasTagCompound() && stack.getTagCompound().getBoolean("isFull")){
				if(vehicle.fluidName.isEmpty() || vehicle.fluidName.equals(stack.getTagCompound().getString("fluidName"))){
					if(vehicle.fuel + 1000 > vehicle.definition.motorized.fuelCapacity){
						player.sendPacket(new PacketPlayerChatMessage("interact.jerrycan.toofull"));
					}else{
						vehicle.fluidName = stack.getTagCompound().getString("fluidName");
						vehicle.fuel += 1000;
						if(!vehicle.world.isClient()){
							stack.setTagCompound(null);
							player.sendPacket(new PacketPlayerChatMessage("interact.jerrycan.success"));
							player.sendPacket(new PacketVehicleInteract("interact.jerrycan.success"));
						}
						
						MT.MTSNet.sendToAll(new PacketVehicleJerrycan(vehicle, vehicle.fluidName));
					}
				}else{
					player.sendPacket(new PacketPlayerChatMessage("interact.jerrycan.wrongtype"));
				}
			}else{
				player.sendPacket(new PacketPlayerChatMessage("interact.jerrycan.empty"));
			}
		}
	}
}
