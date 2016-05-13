package minecraftflightsimulator.items;

import minecraftflightsimulator.MFS;
import net.minecraft.item.Item;

public class ItemSkid extends Item{

	public ItemSkid(){
		this.setUnlocalizedName("Skid");
		this.setCreativeTab(MFS.tabMFS);
		this.setTextureName("mfs:skid");
    }
}
