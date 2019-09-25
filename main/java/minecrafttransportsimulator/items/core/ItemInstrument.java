package minecrafttransportsimulator.items.core;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.List;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemInstrument extends Item{
	public final String instrumentName;
	public final List<String> validVehicles;
	
	public ItemInstrument(String instrumentName){
		super();
		this.instrumentName = instrumentName;
		this.validVehicles = PackParserSystem.getInstrument(instrumentName).general.validVehicles;
		this.setUnlocalizedName(instrumentName.replace(":", "."));
		this.setCreativeTab(MTSRegistry.packTabs.get(instrumentName.substring(0, instrumentName.indexOf(':'))));
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		tooltipLines.add(I18n.format(this.getUnlocalizedName(stack) + ".description"));
	}
}
