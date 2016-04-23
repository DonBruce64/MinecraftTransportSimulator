package minecraftflightsimulator.items;

import minecraftflightsimulator.MFS;
import net.minecraft.item.Item;

public class ItemWheelSmall extends Item{

	public ItemWheelSmall(){
		this.setUnlocalizedName("WheelSmall");
		this.setCreativeTab(MFS.tabMFS);
		this.setTextureName("mfs:wheelsmall");
    }
}
