package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.rendering.components.AParticle;

public class ParticleFlame extends AParticle{
	
	public ParticleFlame(IWrapperWorld world, Point3d position, Point3d motion, float scale){
		super(world, position, motion, 1.0F, 1.0F, 1.0F, 1.0F, scale);
	}
	
	@Override
	public void update(boolean onGround){
		super.update(onGround);
		//Flame just slowly drifts in the direction it was going.
		motion.multiply(0.96);
	}
	
	@Override
	public float getScale(float partialTicks){
		float fineAge = (age + partialTicks)/maxAge;
        return scale*(1.0F - fineAge*fineAge/2F);
	}
	
	@Override
	protected int generateMaxAge(){
		return (int)(8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
	}
	
	@Override
	public int getTextureIndex(){
		return 48;
	}
}
