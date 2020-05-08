package minecrafttransportsimulator.items.packs.parts;

import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONPart;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class ItemPartEngineCar extends AItemPartEngine{
	
	public ItemPartEngineCar(JSONPart definition){
		super(definition);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	protected void addExtraInformation(ItemStack stack, JSONPart pack, List<String> tooltipLines){
		tooltipLines.add(pack.engine.isAutomatic ? I18n.format("info.item.engine.automatic") : I18n.format("info.item.engine.manual"));
		tooltipLines.add(I18n.format("info.item.engine.gearratios"));
		for(byte i=0; i<pack.engine.gearRatios.length; i+=5){
			String gearRatios = "";
			for(byte j=i; j<i+5 && j<pack.engine.gearRatios.length; ++j){
				gearRatios += String.valueOf(pack.engine.gearRatios[j]) + ",  ";
			}
			tooltipLines.add(gearRatios);
		}
	}
}
