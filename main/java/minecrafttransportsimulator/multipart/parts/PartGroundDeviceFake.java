package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;

/**A fake ground device that will be added to the multipart when long ground devices are present.
 * Does not render and will be removed in tandem with the ground device that's linked to it.
 * These longer parts cannot go flat, therefore some methods have been simplified here.
 * 
 * @author don_bruce
 */
public final class PartGroundDeviceFake extends PartGroundDevice{
	
	public PartGroundDeviceFake(EntityMultipartD_Moving multipart, Vec3d offset, boolean isController, boolean turnsWithSteer, String partName, NBTTagCompound dataTag){
		super(multipart, offset, isController, turnsWithSteer, partName, dataTag);
	}
	
	@Override
	public void attackPart(DamageSource source, float damage){}
	
	@Override
	public void updatePart(){}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = new NBTTagCompound();
		return dataTag;
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
		//TODO add fake ground device for pontoons and other ground devices.
		return null;
	}
	
	public float getMotiveFriction(){
		return this.pack.groundDevice.motiveFriction;
	}
	
	public float getLateralFriction(){
		return this.pack.groundDevice.lateralFriction;
	}
}
