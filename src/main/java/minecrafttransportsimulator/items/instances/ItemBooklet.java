package minecrafttransportsimulator.items.instances;

import mcinterface.BuilderGUI;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.guis.instances.GUIBooklet;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONBooklet;

public class ItemBooklet extends AItemPack<JSONBooklet>{
	/*Current page of this booklet.  Kept here locally as only one item class is constructed for each booklet definition.*/
	public int pageNumber;
	
	public ItemBooklet(JSONBooklet definition){
		super(definition);
	}
	
	@Override
	public boolean onUsed(WrapperWorld world, WrapperPlayer player){
		if(world.isClient()){
			BuilderGUI.openGUI(new GUIBooklet(this));
		}
        return true;
    }
	
	@Override
	public String getModelLocation(){
		return null;
	}
	
	@Override
	public String getTextureLocation(){
		return null;
	}
}
