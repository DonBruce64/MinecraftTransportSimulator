package minecrafttransportsimulator.rendering.instances;

import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.rendering.components.AParticle;

public class ParticleDrip extends AParticle{
	private boolean wasOnGround;
	
	public ParticleDrip(WrapperWorld world, Point3d position, Point3d motion, float red, float green, float blue, float scale){
		super(world, position, motion, red, green, blue, 1.0F, scale);
	}
	
	@Override
	public void update(boolean onGround){
		super.update(onGround);
		wasOnGround = onGround;
		//Drips go down to the ground, until the hit it, at which point they stay there for a while.
		motion.multiply(0.96);
		motion.add(0D, -0.06D, 0D);
		if(onGround){
			motion.multiply(0.02);
		}
	}
	
	@Override
	public float getScale(float partialTicks){
        return wasOnGround ? scale*3 : scale;
	}
	
	@Override
	public float getSize(){
		return 0.01F;
	}
	
	@Override
	protected int generateMaxAge(){
		return (int)(64.0D / (Math.random() * 0.8D + 0.2D));
	}
	
	@Override
	public int getTextureIndex(){
		return wasOnGround ? 113 : 112;
	}
}
