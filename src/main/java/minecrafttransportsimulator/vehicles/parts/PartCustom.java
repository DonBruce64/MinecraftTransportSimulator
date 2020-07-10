package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.nbt.NBTTagCompound;

public final class PartCustom extends APart{
	
	public PartCustom(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
	}
	
	@Override
	public NBTTagCompound getData(){
		return new NBTTagCompound();
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
