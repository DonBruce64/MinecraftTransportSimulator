package minecrafttransportsimulator.items.parts;

import java.util.List;

import minecrafttransportsimulator.jsondefs.PackPartObject;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class ItemPartEngineBoat extends AItemPartEngine{
	
	public ItemPartEngineBoat(String partName){
		super(partName);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	protected void addExtraInformation(ItemStack stack, PackPartObject pack, List<String> tooltipLines){}
}
