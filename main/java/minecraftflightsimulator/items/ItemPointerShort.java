package minecraftflightsimulator.items;

import minecraftflightsimulator.MFS;
import net.minecraft.item.Item;

public class ItemPointerShort extends Item{

	public ItemPointerShort(){
		this.setUnlocalizedName("ShortPointer");
		this.setCreativeTab(MFS.tabMFS);
		this.setTextureName("mfs:shortpointer");
	}
}
