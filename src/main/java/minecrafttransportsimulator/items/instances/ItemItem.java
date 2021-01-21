package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUIBooklet;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemFood;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

public class ItemItem extends AItemPack<JSONItem> implements IItemFood{
	/*Current page of this item, if it's a booklet.  Kept here locally as only one item class is constructed for each booklet definition.*/
	public int pageNumber;
	
	public ItemItem(JSONItem definition){
		super(definition);
	}
	
	@Override
	public boolean onUsed(WrapperWorld world, WrapperPlayer player){
		if(world.isClient() && ItemComponentType.BOOKLET.equals(definition.general.type)){
			InterfaceGUI.openGUI(new GUIBooklet(this));
		}
        return true;
    }

	@Override
	public int getTimeToEat(){
		return ItemComponentType.FOOD.equals(definition.general.type) ? definition.food.timeToEat : 0;
	}
	
	@Override
	public boolean isDrink(){
		return definition.food.isDrink;
	}

	@Override
	public int getHungerAmount(){
		return definition.food.hungerAmount;
	}

	@Override
	public float getSaturationAmount(){
		return definition.food.saturationAmount;
	}

	@Override
	public List<JSONPotionEffect> getEffects(){
		return definition.food.effects;
	}
	
	public static enum ItemComponentType{
		@JSONDescription("Creates an item with no functionality.")
		NONE,
		@JSONDescription("Creates a booklet, which is a book-like item.")
		BOOKLET,
		@JSONDescription("Creates an item that can be eaten.")
		FOOD;
	}
}
