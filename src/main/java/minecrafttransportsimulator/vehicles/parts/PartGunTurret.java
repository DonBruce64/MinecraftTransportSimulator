package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

public class PartGunTurret extends APartGun{	
		
	public PartGunTurret(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
	}
	
	@Override
	public Vec3d getActionRotation(float partialTicks){
		//Don't return pitch here, as turrets don't pitch up, only their barrels do.
		return new Vec3d(0, currentYaw - (currentYaw - prevYaw)*(1 - partialTicks), 0);
	}

	@Override
	public float getMinYaw(){
		if (definition.gun.minYaw == 0) return 1;
		else return definition.gun.minYaw;
	}
	
	@Override
	public float getMaxYaw(){
		if (definition.gun.maxYaw == 0) return -1;
		else return definition.gun.maxYaw;
	}
	
	@Override
	public float getMinPitch(){
		if (definition.gun.minPitch == 0) return 0;
		else return definition.gun.minPitch;
	}
	
	@Override
	public float getMaxPitch(){
		if (definition.gun.maxPitch == 0) return 75;
		else return definition.gun.maxPitch;
	}
}
