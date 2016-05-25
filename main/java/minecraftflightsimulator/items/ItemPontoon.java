package minecraftflightsimulator.items;

import minecraftflightsimulator.MFS;
import net.minecraft.item.Item;

public class ItemPontoon extends Item{

	public ItemPontoon(){
		this.setUnlocalizedName("Pontoon");
		this.setCreativeTab(MFS.tabMFS);
		this.setTextureName("mfs:pontoon");
    }
}
