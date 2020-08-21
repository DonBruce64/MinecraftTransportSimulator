package minecrafttransportsimulator.vehicles.parts;

import mcinterface.WrapperNBT;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public final class PartCustom extends APart{
	
	public PartCustom(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, WrapperNBT data){
		super(vehicle, packVehicleDef, definition, data);
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
