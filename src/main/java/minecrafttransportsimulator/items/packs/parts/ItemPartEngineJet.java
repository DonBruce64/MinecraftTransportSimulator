package minecrafttransportsimulator.items.packs.parts;

import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONPart;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class ItemPartEngineJet extends AItemPartEngine{
	
	public ItemPartEngineJet(JSONPart definition){
		super(definition);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	protected void addExtraInformation(ItemStack stack, JSONPart pack, List<String> tooltipLines){
		tooltipLines.add(I18n.format("info.item.engine.bypassratio") + pack.engine.bypassRatio);
	}
}
