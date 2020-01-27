package minecrafttransportsimulator.items.packs.parts;

import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONPart;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class ItemPartEngineBoat extends AItemPartEngine{
	
	public ItemPartEngineBoat(JSONPart definition){
		super(definition);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	protected void addExtraInformation(ItemStack stack, JSONPart pack, List<String> tooltipLines){}
}
