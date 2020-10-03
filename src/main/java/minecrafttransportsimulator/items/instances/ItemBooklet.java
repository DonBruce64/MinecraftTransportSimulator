package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.guis.instances.GUIBooklet;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONBooklet;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;

public class ItemBooklet extends AItemPack<JSONBooklet>{
	/*Current page of this booklet.  Kept here locally as only one item class is constructed for each booklet definition.*/
	public int pageNumber;
	
	public ItemBooklet(JSONBooklet definition){
		super(definition);
	}
	
	@Override
	public boolean onUsed(IWrapperWorld world, IWrapperPlayer player){
		if(world.isClient()){
			MasterLoader.guiInterface.openGUI(new GUIBooklet(this));
		}
        return true;
    }
}
