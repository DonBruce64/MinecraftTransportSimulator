package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.nbt.NBTTagCompound;

public class PartGunTripod extends APartGun{	
		
	public PartGunTripod(EntityVehicleE_Powered vehicle, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
	}
	
	@Override
	public float getMinYaw(){
		return -45;
	}
	
	@Override
	public float getMaxYaw(){
		return 45;
	}
	
	@Override
	public float getMinPitch(){
		return -35;
	}
	
	@Override
	public float getMaxPitch(){
		return 35;
	}
}
