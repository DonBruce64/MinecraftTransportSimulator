package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleJerrycan;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
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
		tooltipLines.add(I18n.format("info.item.jerrycan.fill"));
		tooltipLines.add(I18n.format("info.item.jerrycan.drain"));
		if(stack.hasTagCompound() && stack.getTagCompound().getBoolean("isFull")){
			tooltipLines.add(I18n.format("info.item.jerrycan.contains") + stack.getTagCompound().getString("fluidName"));
		}else{
			tooltipLines.add(I18n.format("info.item.jerrycan.empty"));
		}
	}
	
	@Override
	public void doVehicleInteraction(ItemStack stack, EntityVehicleE_Powered vehicle, APart part, EntityPlayerMP player, PlayerOwnerState ownerState, boolean rightClick){
		if(rightClick){
			if(stack.hasTagCompound() && stack.getTagCompound().getBoolean("isFull")){
				if(vehicle.fluidName.isEmpty() || vehicle.fluidName.equals(stack.getTagCompound().getString("fluidName"))){
					if(vehicle.fuel + 1000 > vehicle.definition.motorized.fuelCapacity){
						MTS.MTSNet.sendTo(new PacketChat("interact.jerrycan.toofull"), player);
					}else{
						vehicle.fluidName = stack.getTagCompound().getString("fluidName");
						vehicle.fuel += 1000;
						stack.setTagCompound(null);
						MTS.MTSNet.sendTo(new PacketChat("interact.jerrycan.success"), player);
						MTS.MTSNet.sendToAll(new PacketVehicleJerrycan(vehicle, vehicle.fluidName));
					}
				}else{
					MTS.MTSNet.sendTo(new PacketChat("interact.jerrycan.wrongtype"), player);
				}
			}else{
				MTS.MTSNet.sendTo(new PacketChat("interact.jerrycan.empty"), player);
			}
		}
	}
}
