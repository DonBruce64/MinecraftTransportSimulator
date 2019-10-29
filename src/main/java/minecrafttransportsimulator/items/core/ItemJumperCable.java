package minecrafttransportsimulator.items.core;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.List;

import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemJumperCable extends Item{
	public static APartEngine lastEngineClicked;
	
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
