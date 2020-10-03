package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public final class PartCustom extends APart{
	
	public PartCustom(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, IWrapperNBT data, APart parentPart){
		super(vehicle, packVehicleDef, definition, data, parentPart);
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
