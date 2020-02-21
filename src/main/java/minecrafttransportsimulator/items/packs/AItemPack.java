package minecrafttransportsimulator.items.packs;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.jsondefs.AJSONItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**Base item class for all pack-created items.  Stores information such as the
 * pack the item belongs to and the class that extends {@link AJSONItem} that
 * is the instance of the item's pack.
 * 
 * @author don_bruce
 */
public abstract class AItemPack<JSONDefinition extends AJSONItem<? extends AJSONItem<?>.General>> extends Item{
	public final JSONDefinition definition;
	
	public AItemPack(JSONDefinition definition){
		super();
		this.definition = definition;
	}
	
	@Override
	public String getItemStackDisplayName(ItemStack stack){
        return definition.general.name != null ? definition.general.name : definition.systemName;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		if(definition.general.description != null){
			for(String tooltipLine : definition.general.description.split("\n")){
				tooltipLines.add(tooltipLine);
			}
		}
	}
}
