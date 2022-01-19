package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.items.instances.ItemPartGroundDevice;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;

/**A fake ground device that will be added to the vehicle when long ground devices are present.
 * Does not render and will be removed in tandem with the ground device that's linked to it.
 * 
 * @author don_bruce
 */
public final class PartGroundDeviceFake extends PartGroundDevice{
	private final PartGroundDevice masterPart;
	
	public PartGroundDeviceFake(PartGroundDevice masterPart, WrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(masterPart.entityOn, placingPlayer, placementDefinition, data, parentPart);
		this.masterPart = masterPart;
	}
	
	@Override
	public boolean isFake(){
		return true;
	}
	
	@Override
	public float getWidth(){
		return masterPart != null ? masterPart.getWidth() : 1.0F;
	}
	
	@Override
	public float getHeight(){
		return masterPart != null ? masterPart.getHeight() : 1.0F;
	}
	
	@Override
	public ItemPartGroundDevice getItem(){
		return null;
	}
	
	@Override
	public float getLongPartOffset(){
		return -masterPart.getLongPartOffset();
	}
}
