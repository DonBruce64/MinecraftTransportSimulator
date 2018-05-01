package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.entities.core.EntityMultipart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

/**Any child that touches the ground should extend this class.
 * It's used to perform ground physics and rendering.
 * 
 * @author don_bruce
 */
public abstract class AMultipartGroundDevice extends AMultipartPart{
	public final float motiveFriction;
	public final float lateralFriction;
	
	private static final Vec3d groundDetectionOffset = new Vec3d(0, -0.05F, 0);
	
	public AMultipartGroundDevice(EntityMultipart multipart, Vec3d offset, boolean isController, boolean turnsWithSteer, NBTTagCompound dataTag){
		super(multipart, offset, isController, turnsWithSteer, dataTag);
		this.motiveFriction = dataTag.getFloat("motiveFriction");
		this.lateralFriction = dataTag.getFloat("lateralFriction");
	}
	
	@Override
	public void setPartNBTToTag(NBTTagCompound tagCompound){
		tagCompound.setFloat("motiveFriction", this.motiveFriction);
		tagCompound.setFloat("lateralFriction", this.lateralFriction);
	}
	
	/**Checks to see if this part is on the ground.
	 * Used to see if this ground device needs to affect physics.
	 */
	public boolean isOnGround(){
		return isPartCollidingWithBlocks(groundDetectionOffset);
	}
}
