package mcinterface1122;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.rendering.components.AParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;

/**Builder for a MC particles.  This builder doesn't do much that the default implementation
 * already does, so it's mainly a way to avoid any MC changes down the line.
 *
 * @author don_bruce
 */
class BuilderParticle extends Particle{
	private final AParticle particle;
	
    BuilderParticle(AParticle particle){
		super(((WrapperWorld) particle.world).world, particle.position.x, particle.position.y, particle.position.z);
		this.particle = particle;
		this.particleMaxAge = particle.maxAge;
		this.motionX = particle.motion.x;
		this.motionY = particle.motion.y;
		this.motionZ = particle.motion.z;
		this.setSize(particle.getSize(), particle.getSize());
		this.setBoundingBox(new AxisAlignedBB(posX - width/2F, posY - height/2F, posZ - width/2F, posX + width/2F, posY + height/2F, posZ + width/2F));
	}
    
    @Override
	public void onUpdate(){
    	if(particle.age == particle.maxAge){
    		setExpired();
    	}else{			
			//First, update the prev position variables.
			prevPosX = particle.position.x;
			prevPosY = particle.position.y;
			prevPosZ = particle.position.z;
			
			//Do particle update logic.
			particle.update(onGround);
			motionX = particle.motion.x;
			motionY = particle.motion.y;
	        motionZ = particle.motion.z;
			
			//Try to move the particle.
			move(motionX, motionY, motionZ);
			
			//Send position and motion back to the particle class.
			//This ensures the particle knows if it didn't move.
			particle.position.set(posX, posY, posZ);
			particle.motion.set(motionX, motionY, motionZ);
		}
    }
    
    @Override
	public int getBrightnessForRender(float partialTicks){
        if(particle.isBright()){
			int i = super.getBrightnessForRender(partialTicks);
		    int k = i >> 16 & 255;
		    return 240 | k << 16;
        }else{
        	return super.getBrightnessForRender(partialTicks);
        }
    }
    
    @Override
    public int getFXLayer(){
        return particle.getTextureIndex() == -1 ? 3 : 0;
    }
    
    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ){
    	//Need to set rendering parameters before rendering.
    	particleRed = particle.red;
		particleGreen = particle.green;
		particleBlue = particle.blue;
		particleAlpha = particle.alpha;
    	particleScale = particle.getScale(partialTicks);
    	if(getFXLayer() == 0){
	    	setParticleTextureIndex(particle.getTextureIndex());
	    	super.renderParticle(buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    	}else{
    		//Translate by the rendering offset.
    		GL11.glPushMatrix();
            GL11.glTranslated(
        		prevPosX + (posX - prevPosX) * partialTicks - interpPosX,
        		prevPosY + (posY - prevPosY) * partialTicks - interpPosY,
        		prevPosZ + (posZ - prevPosZ) * partialTicks - interpPosZ
        	);
            
            //Set brightness.
    	    MasterInterface.renderInterface.setLightingToBlock(new Point3i(particle.position));
    	    if(particle.isBright()){
    	    	MasterInterface.renderInterface.setLightingState(false);
    	    }
    	    
    	    //Render.
    		particle.render(partialTicks);
    		
    		//Pop the matrix.
    		GL11.glPopMatrix();
    	}
    }
}
