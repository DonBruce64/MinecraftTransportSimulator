package minecrafttransportsimulator.entities.main;

import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**Any child that touches the ground should extend this class.
 * It's used to perform ground physics and rendering.
 * 
 * @author don_bruce
 */
public abstract class EntityGroundDevice extends EntityMultipartChild{
	public float motiveFriction;
	public float lateralFriction;
	
	public EntityGroundDevice(World world){
		super(world);
	}
	
	public EntityGroundDevice(World world, EntityMultipartMoving mover, String parentUUID, float offsetX, float offsetY, float offsetZ, float width, float height, float motiveFriction, float lateralFriction){
		super(world, mover, parentUUID, offsetX, offsetY, offsetZ, width, height, 0);
		this.motiveFriction = motiveFriction;
		this.lateralFriction = lateralFriction;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.motiveFriction=tagCompound.getFloat("motiveFriction");
		this.lateralFriction=tagCompound.getFloat("lateralFriction");
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setFloat("motiveFriction", this.motiveFriction);
		tagCompound.setFloat("lateralFriction", this.lateralFriction);
	}
}
