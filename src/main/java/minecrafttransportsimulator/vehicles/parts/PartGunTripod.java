package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.nbt.NBTTagCompound;

public class PartGunTripod extends APartGun{	
		
	public PartGunTripod(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
	}
	
	@Override
	public float getMinYaw(){
		if (definition.gun.minYaw == 0) return -45;
		else return definition.gun.minYaw;
	}
	
	@Override
	public float getMaxYaw(){
		if (definition.gun.maxYaw == 0) return 45;
		else return definition.gun.maxYaw;
	}
	
	@Override
	public float getMinPitch(){
		if (definition.gun.minPitch == 0) return -35;
		else return definition.gun.minPitch;
	}
	
	@Override
	public float getMaxPitch(){
		if (definition.gun.maxPitch == 0) return 35;
		else return definition.gun.maxPitch;
	}
}
