package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public final class PartGeneric extends APart{
	
	public PartGeneric(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, ItemPart item, IWrapperNBT data, APart parentPart){
		super(vehicle, packVehicleDef, item, data, parentPart);
	}
	
	@Override
	public float getWidth(){
		return definition.generic.width;
	}

	@Override
	public float getHeight(){
		return definition.generic.height;
	}
}
