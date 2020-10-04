package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public final class PartCustom extends APart{
	
	public PartCustom(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, ItemPart item, IWrapperNBT data, APart parentPart){
		super(vehicle, packVehicleDef, item, data, parentPart);
	}
	
	@Override
	public float getWidth(){
		return definition.custom.width;
	}

	@Override
	public float getHeight(){
		return definition.custom.height;
	}
}
