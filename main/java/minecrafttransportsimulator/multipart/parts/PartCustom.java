package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import net.minecraft.nbt.NBTTagCompound;

public final class PartCustom extends APart{
	
	public PartCustom(EntityMultipartD_Moving multipart, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(multipart, packPart, partName, dataTag);
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		return new NBTTagCompound();
	}
	
	@Override
	public float getWidth(){
		return pack.custom.width;
	}

	@Override
	public float getHeight(){
		return pack.custom.height;
	}
}
