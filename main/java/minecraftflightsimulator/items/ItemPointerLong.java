package minecraftflightsimulator.items;

import minecraftflightsimulator.MFS;
import net.minecraft.item.Item;

public class ItemPointerLong extends Item{

	public ItemPointerLong(){
		this.setUnlocalizedName("LongPointer");
		this.setCreativeTab(MFS.tabMFS);
		this.setTextureName("mfs:longpointer");
	}
}
