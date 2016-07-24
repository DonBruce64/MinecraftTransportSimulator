package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityFlyable;
import minecraftflightsimulator.entities.core.EntityLandingGear;
import net.minecraft.world.World;

public abstract class EntityWheel extends EntityLandingGear{
	private EntityFlyable flyer;
	public float angularPosition;
	public float angularVelocity;
	protected float wheelDiameter;
	
	public EntityWheel(World world){
		super(world);
	}
	
	public EntityWheel(World world, EntityFlyable flyer, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, flyer, parentUUID, offsetX, offsetY, offsetZ);
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		flyer = (EntityFlyable) this.parent;
				
		if(worldObj.isRemote){
			if(this.isOnGround()){
				angularVelocity = (float) (flyer.velocity/wheelDiameter);
			}else{
				if(flyer.brakeOn || flyer.parkingBrakeOn){
					angularVelocity = 0;
				}else if(angularVelocity>0){
					angularVelocity = (float) Math.max(angularVelocity - 0.05, 0);
				}
			}
			angularPosition += angularVelocity;
		}
	}
}
