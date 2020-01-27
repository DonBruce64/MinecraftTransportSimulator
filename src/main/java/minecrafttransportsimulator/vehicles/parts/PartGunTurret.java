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
		return -180;
	}
	
	@Override
	public float getMaxYaw(){
		return 180;
	}
	
	@Override
	public float getMinPitch(){
		return -75;
	}
	
	@Override
	public float getMaxPitch(){
		return 0;
	}
}
