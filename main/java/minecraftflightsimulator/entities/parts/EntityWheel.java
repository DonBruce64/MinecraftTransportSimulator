package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityLandingGear;
import minecraftflightsimulator.entities.core.EntityParent;
import net.minecraft.world.World;

public abstract class EntityWheel extends EntityLandingGear{
	public float angularPosition;
	public float angularVelocity;
	protected float wheelDiameter;
	
	public EntityWheel(World world){
		super(world);
	}
	
	public EntityWheel(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ);
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		if(worldObj.isRemote){
			if(this.isOnGround()){
				angularVelocity = (float) (parent.velocity/wheelDiameter);
			}else{
				if(parent.brakeOn || parent.parkingBrakeOn){
					angularVelocity = 0;
				}else if(angularVelocity>0){
					angularVelocity = (float) Math.max(angularVelocity - 0.05, 0);
				}
			}
			angularPosition += angularVelocity;
		}
	}
}
