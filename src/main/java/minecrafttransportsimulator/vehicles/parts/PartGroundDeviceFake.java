package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**A fake ground device that will be added to the vehicle when long ground devices are present.
 * Does not render and will be removed in tandem with the ground device that's linked to it.
 * 
 * @author don_bruce
 */
public final class PartGroundDeviceFake extends PartGroundDevice{
	private final PartGroundDevice masterPart;
	
	public PartGroundDeviceFake(PartGroundDevice masterPart, VehiclePart packVehicleDef, ItemPart item, WrapperNBT data, APart parentPart){
		super(masterPart.vehicle, packVehicleDef, item, data, parentPart);
		this.masterPart = masterPart;
	}
	
	@Override
	public boolean isFake(){
		return true;
	}
	
	@Override
	public void remove(){
		//Do nothing here as we should not be removing ourselves from the vehicle.
		//That is our master part's job.
	}
	
	@Override
	public WrapperNBT getData(){
		return new WrapperNBT();
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
	public ItemPart getItem(){
		return null;
	}
	
	@Override
	public float getLongPartOffset(){
		return -masterPart.getLongPartOffset();
	}
}
