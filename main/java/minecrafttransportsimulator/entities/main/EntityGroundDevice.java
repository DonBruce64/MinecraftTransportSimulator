package minecrafttransportsimulator.entities.main;

import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import net.minecraft.world.World;

/**Any child that touches the ground should extend this class.
 * It's used to perform ground physics and rendering.
 * 
 * @author don_bruce
 */
public abstract class EntityGroundDevice extends EntityMultipartChild{
	
	public EntityGroundDevice(World world){
		super(world);
	}
	
	public EntityGroundDevice(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, 0);
	}
	
	public boolean shouldAffectSteering(){
		return this.turnsWithSteer;
	}
	
	public abstract float getMotiveFriction();
	
	public abstract float getLateralFriction();
}
