package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

public final class PartCustom extends APart{
	
	public PartCustom(EntityMultipartD_Moving multipart, Vec3d offset, boolean isController, boolean turnsWithSteer, String partName, NBTTagCompound dataTag){
		super(multipart, offset, isController, turnsWithSteer, partName, dataTag);
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
