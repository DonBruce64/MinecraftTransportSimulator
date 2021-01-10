package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONParticleObject;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.components.AParticle;

public class ParticleSuspendedSmoke extends AParticle {

	public ParticleSuspendedSmoke(WrapperWorld world, Point3d position, Point3d motion, JSONParticleObject JSONParticleObject){
		super(world, position, motion.add(new Point3d(0.04*Math.random(), 0.04*Math.random(), 0.04*Math.random())), JSONParticleObject);
	}
	
	@Override
	public void update(){
		super.update();
		motion.multiply(0.95);
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
		return definition.duration == 0 ? (int)(8.0D / (Math.random() * 0.8D + 0.2D)) : definition.duration;
	}
	
	@Override
	public int getTextureIndex(){
		//Smoke gets smaller as it ages.
		return (int)(7 - 8F*age/maxAge);
	}
}
