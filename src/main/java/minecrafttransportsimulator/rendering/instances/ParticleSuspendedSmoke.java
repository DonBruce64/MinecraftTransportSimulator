package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart.ParticleObject;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.rendering.components.AParticle;

public class ParticleSuspendedSmoke extends AParticle {

	public ParticleSuspendedSmoke(IWrapperWorld world, Point3d position, Point3d motion, ParticleObject particleObject){
		super(world, position, motion.add(new Point3d(0.04*Math.random(), 0.04*Math.random(), 0.04*Math.random())), particleObject);
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
		return particleObject.duration == 0 ? (int)(8.0D / (Math.random() * 0.8D + 0.2D)) : particleObject.duration;
	}
	
	@Override
	public int getTextureIndex(){
		//Smoke gets smaller as it ages.
		return (int)(7 - 8F*age/maxAge);
	}
}
