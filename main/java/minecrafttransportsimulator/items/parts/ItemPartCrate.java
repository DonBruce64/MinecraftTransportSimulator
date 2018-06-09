package minecrafttransportsimulator.items.parts;

public class ItemPartCrate extends AItemPart{
	
	public ItemPartCrate(String partName){
		super(partName);
	}
	
	@Override
	public boolean isPartValueInRange(float minValue, float maxValue){
		return true;
	}
}
