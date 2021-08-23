package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.entities.instances.EntityParticle;
import minecrafttransportsimulator.jsondefs.JSONParticle.ParticleType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**Builder for a MC particles.  This builder doesn't do much that the default implementation
 * already does, so it's mainly a way to avoid any MC changes down the line.
 *
 * @author don_bruce
 */
public class BuilderParticle extends Particle{
	private final EntityParticle particle;
	private static final TextureAtlasSprite FAKE_SPRITE = new FakeTextureAtlasSprite();
	private static final ResourceLocation PARTICLE_TEXTURES = new ResourceLocation("textures/particle/particles.png");
	
    public BuilderParticle(EntityParticle particle){
		super(particle.world.world, particle.position.x, particle.position.y, particle.position.z);
		this.particle = particle;
		this.particleMaxAge = particle.maxAge;
		this.motionX = particle.motion.x;
		this.motionY = particle.motion.y;
		this.motionZ = particle.motion.z;
		this.setSize(particle.getSize(), particle.getSize());
		this.setBoundingBox(new AxisAlignedBB(posX - width/2F, posY - height/2F, posZ - width/2F, posX + width/2F, posY + height/2F, posZ + width/2F));
		if(particle.definition.type.equals(ParticleType.BREAK)){
			BlockPos belowPos = new BlockPos(particle.position.x, particle.position.y - 1, particle.position.z);
			IBlockState belowState = world.getBlockState(belowPos);
			this.setParticleTexture(Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getTexture(belowState));
		}else if(particle.definition.texture != null){
			this.particleTexture = FAKE_SPRITE;
		}
	}
    
    @Override
	public void onUpdate(){
    	if(particle.age == particle.maxAge || !particle.isValid){
    		setExpired();
    	}else{			
			//First, update the prev position variables.
			prevPosX = particle.position.x;
			prevPosY = particle.position.y;
			prevPosZ = particle.position.z;
			
			//Do particle update logic, and set new position/motion.
			particle.update();
			posX = particle.position.x;
			posY = particle.position.y;
			posZ = particle.position.z;
			motionX = particle.motion.x;
			motionY = particle.motion.y;
	        motionZ = particle.motion.z;
	        
	        /* Sets the packed UV index for this texture.
			 * The packed UV index is a single integer divided by 16, with the
			 * quotient of the value being the U value, and the remainder
			 * being the V value.  So for example, a texture index of 22
			 * would have a quotient of 1, and a remainder of 6, so would have
			 * a UV of 1,6.
			 */
			switch(particle.definition.type){
				case SMOKE: setParticleTextureIndex(7 - particle.age*8/particle.maxAge); break;//Smoke gets smaller as it ages.
				case FLAME: setParticleTextureIndex(48); break;
				case DRIP: setParticleTextureIndex(particle.touchingBlocks ? 113 : 112); break;//Drips become flat when they hit the ground.
				case BUBBLE: setParticleTextureIndex(32); break;
				case BREAK: break;//Do nothing, as breaking particles don't use the normal texture.
				case GENERIC: break;//Do nothing, as generic particles don't use the normal texture.
			}
		}
    }
    
    @Override
	public int getBrightnessForRender(float partialTicks){
        if(particle.definition.type.equals(ParticleType.FLAME)){
			int i = super.getBrightnessForRender(partialTicks);
		    int k = i >> 16 & 255;
		    return 240 | k << 16;
        }else{
        	return super.getBrightnessForRender(partialTicks);
        }
    }
    
    @Override
    public int getFXLayer(){
        return particle.definition.type.equals(ParticleType.BREAK) ? 1 : 0;
    }
    
    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ){
    	//Need to set rendering parameters before rendering.
    	particleRed = particle.getRed();
		particleGreen = particle.getGreen();
		particleBlue = particle.getBlue();
		particleAlpha = particle.getAlpha();
    	particleScale = particle.getScale(partialTicks);
    	if(particle.definition.texture != null){
    		//Draw what we have in the buffer and then bind our new texture and draw it.
    		Tessellator.getInstance().draw();
    		buffer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
    		InterfaceRender.bindTexture(particle.definition.texture);
    		super.renderParticle(buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    		
    		//Set the buffer back up for default particle rendering for the next particle.
    		Tessellator.getInstance().draw();
    		buffer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
    		Minecraft.getMinecraft().renderEngine.bindTexture(PARTICLE_TEXTURES);
    	}else{
    		super.renderParticle(buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    	}
    }
    
    private static class FakeTextureAtlasSprite extends TextureAtlasSprite{
    	private FakeTextureAtlasSprite(){
    		super(MasterLoader.MODID + ":" + "particle");
    		this.width = 32;
    		this.height = 32;
    		initSprite(32, 32, 0, 0, false);
    	}
    }
}
