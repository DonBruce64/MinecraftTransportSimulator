package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.nbt.NBTTagCompound;

public final class PartGroundDeviceTread extends APartGroundDevice{
	public PartGroundDeviceTread(EntityVehicleE_Powered vehicle, VehiclePart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
	}	
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		return new NBTTagCompound();
	}
	
	@Override
	public float getWidth(){
		return this.pack.tread.width;
	}
	
	@Override
	public float getHeight(){
		return this.getWidth();
	}
	
	@Override
	public float getMotiveFriction(){
		return this.pack.tread.motiveFriction;
	}
	
	@Override
	public float getLateralFriction(){
		return this.pack.tread.lateralFriction;
	}
	
	@Override
	public float getLongPartOffset(){
		return pack.tread.extraCollisionBoxOffset;
	}
	
	@Override
	public boolean canBeDrivenByEngine(){
		return true;
	}
}
