package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.guis.instances.GUIVehicleEditor;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemWrench extends Item{
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
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand){
		if(ConfigSystem.configObject.client.devMode.value && world.isRemote){
			if(player.getRidingEntity() instanceof EntityVehicleE_Powered){
				WrapperGUI.openGUI(new GUIVehicleEditor((EntityVehicleE_Powered) player.getRidingEntity()));
			}
		}
		return new ActionResult<ItemStack>(EnumActionResult.PASS, player.getHeldItem(hand));
    }
}
