package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.rendering.components.AParticle;

public class ParticleSmoke extends AParticle{
	
	public ParticleSmoke(IWrapperWorld world, Point3d position, Point3d motion, float red, float green, float blue, float alpha, float scale){
		super(world, position, motion, red, green, blue, alpha, scale);
	}
	
	@Override
	public void update(){
		super.update();
		//Update the motions to make the smoke float up.
		motion.x *= 0.9;
		motion.y += 0.004;
		motion.z *= 0.9;
	}
	
	@Override
	public float getScale(float partialTicks){
		return scale;
	}
	
	@Override
	public float getSize(){
		return 0.02F;
	}
	
	@Override
	protected int generateMaxAge(){
		return (int)(8.0D / (Math.random() * 0.8D + 0.2D));
	}
	
	@Override
	public int getTextureIndex(){
		//Smoke gets smaller as it ages.
		return 7 - 8*age/maxAge;
	}
}
