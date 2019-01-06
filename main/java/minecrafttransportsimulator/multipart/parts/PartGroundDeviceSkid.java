package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import net.minecraft.nbt.NBTTagCompound;

public final class PartGroundDeviceSkid extends APartGroundDevice{
	public PartGroundDeviceSkid(EntityMultipartD_Moving multipart, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(multipart, packPart, partName, dataTag);
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		return new NBTTagCompound();
	}
	
	@Override
	public float getWidth(){
		return this.pack.skid.width;
	}
	
	@Override
	public float getHeight(){
		return this.getWidth();
	}
	
	@Override
	public float getMotiveFriction(){
		return 0;
	}
	
	@Override
	public float getLateralFriction(){
		return this.pack.skid.lateralFriction;
	}
	
	@Override
	public float getLongPartOffset(){
		return 0;
	}
	
	@Override
	public boolean canBeDrivenByEngine(){
		return false;
	}
}
