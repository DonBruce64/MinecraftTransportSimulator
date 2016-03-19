package minecraftflightsimulator.items;

import minecraftflightsimulator.MFS;
import net.minecraft.item.Item;

public class ItemFlightInstrumentBase extends Item{

	public ItemFlightInstrumentBase(){
		this.setUnlocalizedName("FlightInstrumentBase");
		this.setCreativeTab(MFS.tabMFS);
		this.setTextureName("mfs:flightinstrumentbase");
	}
}
