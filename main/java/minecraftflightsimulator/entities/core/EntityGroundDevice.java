package minecraftflightsimulator.entities.core;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**Any child that touches the ground should extend this class.
 * It's used to perform ground physics and rendering.
 * 
 * @author don_bruce
 */
public abstract class EntityGroundDevice extends EntityChild{
	public boolean turnsWithVehicle;
	public boolean isRetractable;
	public boolean isRetracted;
	public float motiveFriction;
	public float lateralFriction;
	public float extendedX;
	public float extendedY;
	public float extendedZ;
	public float retractedX;
	public float retractedY;
	public float retractedZ;
	
	public EntityGroundDevice(World world){
		super(world);
	}
	
	public EntityGroundDevice(World world, EntityVehicle vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ, float width, float height, float motiveFriction, float lateralFriction){
		super(world, vehicle, parentUUID, offsetX, offsetY, offsetZ, width, height, 0);
		this.motiveFriction = motiveFriction;
		this.lateralFriction = lateralFriction;
	}
	
	/**Use this to set the properties of the grounded entity.
	 * Should be called right after the constructor and before spawning entity.
	 * 
	 * @param turnsWithVehicle
	 * @param retractable
	 * @param extendedCoords
	 * @param retractedCoords
	 */
	public void setExtraProperties(boolean turnsWithVehicle, boolean retractable, float[] extendedCoords, float[] retractedCoords){
		this.turnsWithVehicle = turnsWithVehicle;
		this.isRetractable = retractable;
		if(retractable){
			this.extendedX = extendedCoords[0];
			this.extendedY = extendedCoords[1];
			this.extendedZ = extendedCoords[2];
			this.retractedX = retractedCoords[0];
			this.retractedY = retractedCoords[1];
			this.retractedZ = retractedCoords[2];
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.turnsWithVehicle=tagCompound.getBoolean("turnsWithVehicle");
		this.isRetractable=tagCompound.getBoolean("isRetractable");
		this.isRetracted=tagCompound.getBoolean("isRetracted");
		this.motiveFriction=tagCompound.getFloat("motiveFriction");
		this.lateralFriction=tagCompound.getFloat("lateralFriction");
		if(isRetractable){
			this.extendedX=tagCompound.getFloat("extendedX");
			this.extendedY=tagCompound.getFloat("extendedY");
			this.extendedZ=tagCompound.getFloat("extendedZ");
			this.retractedX=tagCompound.getFloat("retractedX");
			this.retractedY=tagCompound.getFloat("retractedY");
			this.retractedZ=tagCompound.getFloat("retractedZ");
		}
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("turnsWithVehicle", this.turnsWithVehicle);
		tagCompound.setBoolean("isRetractable", this.isRetractable);
		tagCompound.setBoolean("isRetracted", this.isRetracted);
		tagCompound.setFloat("motiveFriction", this.motiveFriction);
		tagCompound.setFloat("lateralFriction", this.lateralFriction);
		if(isRetractable){
			tagCompound.setFloat("extendedX", this.extendedX);
			tagCompound.setFloat("extendedY", this.extendedY);
			tagCompound.setFloat("extendedZ", this.extendedZ);
			tagCompound.setFloat("retractedX", this.retractedX);
			tagCompound.setFloat("retractedY", this.retractedY);
			tagCompound.setFloat("retractedZ", this.retractedZ);
		}
	}
}
