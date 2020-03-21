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
		return definition.gun.minYaw == 0 ? -45 : definition.gun.minYaw;
	}
	
	@Override
	public float getMaxYaw(){
		return definition.gun.maxYaw == 0 ? 45 : definition.gun.maxYaw;
	}
	
	@Override
	public float getMinPitch(){
		return definition.gun.minPitch == 0 ? -35 : definition.gun.minPitch;
	}
	
	@Override
	public float getMaxPitch(){
		return definition.gun.maxPitch == 0 ? 35 : definition.gun.maxPitch;
	}
}
