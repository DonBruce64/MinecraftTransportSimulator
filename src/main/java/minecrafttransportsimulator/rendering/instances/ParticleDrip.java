package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.components.AParticle;

public class ParticleDrip extends AParticle{
	
	public ParticleDrip(WrapperWorld world, Point3d position, Point3d motion, float red, float green, float blue, float scale){
		super(world, position, motion, red, green, blue, 1.0F, scale);
	}
	
	@Override
	public void update(){
		super.update();
		//Keep moving until we touch a block, then stop.
		if(!touchingBlocks){
			motion.multiply(0.96).add(0D, -0.06D, 0D);
		}else{
			motion.multiply(0.0);
		}
	}
	
	@Override
	public float getScale(float partialTicks){
        return touchingBlocks ? scale*3 : scale;
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
		return touchingBlocks ? 113 : 112;
	}
}
