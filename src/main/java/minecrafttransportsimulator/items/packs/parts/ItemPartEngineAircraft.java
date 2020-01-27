package minecrafttransportsimulator.items.packs.parts;

import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONPart;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class ItemPartEngineAircraft extends AItemPartEngine{
	
	public ItemPartEngineAircraft(JSONPart definition){
		super(definition);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	protected void addExtraInformation(ItemStack stack, JSONPart pack, List<String> tooltipLines){
		tooltipLines.add(I18n.format("info.item.engine.gearratios") + pack.engine.gearRatios[0]);
	}
}
