package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemJumperCable extends Item{
	public static APartEngine<? extends EntityVehicleE_Powered> lastEngineClicked;
	
	public ItemJumperCable(){
		super();
		setFull3D();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		for(byte i=1; i<=5; ++i){
			tooltipLines.add(I18n.format("info.item.jumpercable.line" + String.valueOf(i)));
		}
	}
}
