package minecrafttransportsimulator.multipart.parts;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

/**A fake ground device that will be added to the multipart when long ground devices are present.
 * Does not render and will be removed in tandem with the ground device that's linked to it.
 * These longer parts cannot go flat, therefore some methods have been simplified here.
 * Additionally, the constructor differs to prevent infinite loops.  This is not an issue
 * as the fake part does not use NBT as it's only a collision bit.  It is also not
 * reloaded from NBT from multiparts, rather it is re-created when the master fake
 * part is created.  This means that partName should NOT match any registered
 * parts otherwise bad things will happen with lookups.
 * 
 * @author don_bruce
 */
public final class PartGroundDeviceFake extends PartGroundDevice{
	private final PartGroundDevice masterPart;
	
	public PartGroundDeviceFake(PartGroundDevice masterPart, Vec3d offset, String partName, NBTTagCompound dataTag){
		super(masterPart.multipart, offset, false, false, partName, dataTag);
		this.masterPart = masterPart;
	}
	
	@Override
	public void attackPart(DamageSource source, float damage){}
	
	@Override
	public void updatePart(){}
	
	@Override
	public void removePart(){
		if(this.masterPart != null){
			multipart.removePart(masterPart, false);
		}
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		return new NBTTagCompound();
	}
	
	@Override
	public float getWidth(){
		return this.pack.groundDevice.thickness;
	}
	
	@Override
	public float getHeight(){
		return this.pack.groundDevice.diameter;
	}
	
	@Override
	public Item getItemForPart(){
		return null;
	}
	
	@Override
	public ResourceLocation getModelLocation(){
		return null;
	}
	
	@Override
	public ResourceLocation getTextureLocation(){
		return null;
	}
	
	@Override
	public float getMotiveFriction(){
		return this.pack.groundDevice.motiveFriction;
	}
	
	@Override
	public float getLateralFriction(){
		return this.pack.groundDevice.lateralFriction;
	}
}
