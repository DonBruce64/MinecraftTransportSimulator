package minecrafttransportsimulator.items.parts;

public class ItemPartSeat extends AItemPart{
	
	public ItemPartSeat(String partName){
		super(partName);
	}
	
	@Override
	public boolean isPartValueInRange(float minValue, float maxValue){
		return true;
	}
}
