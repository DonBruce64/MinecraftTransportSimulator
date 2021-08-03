package minecrafttransportsimulator.entities.instances;

import java.awt.Color;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONParticle;
import minecrafttransportsimulator.jsondefs.JSONParticle.ParticleType;
import minecrafttransportsimulator.mcinterface.InterfaceRender;

/**Basic particle class.  This mimic's MC's particle logic, except we can manually set
 * movement logic.  Particles may be spawned by calling {@link InterfaceRender#spawnParticle(EntityParticle)}
 *
 * @author don_bruce
 */
public class EntityParticle extends AEntityB_Existing{
	
	//Constant properties.
	public final JSONParticle definition;
	public final int maxAge;
	
	private final Color startColor;
	private final Color endColor;
	private final Color staticColor;

	//Runtime variables.
	public boolean touchingBlocks;
	public int age;

	public EntityParticle(AEntityC_Definable<?> entitySpawning, JSONParticle definition){
		super(entitySpawning.world, entitySpawning.position, ZERO_FOR_CONSTRUCTOR, ZERO_FOR_CONSTRUCTOR);
		if(definition.pos != null){
			position.add(definition.pos.copy().rotateFine(entitySpawning.angles));
		}
		if(definition.initialVelocity != null){
			//Set initial velocity, but add some randomness so particles don't all go in a line.
			Point3d adjustedVelocity = definition.initialVelocity.copy().rotateFine(entitySpawning.angles);
			motion.x += adjustedVelocity.x/10D + 0.02 - Math.random()*0.04;
			motion.y += adjustedVelocity.y/10D + 0.02 - Math.random()*0.04;
			motion.z += adjustedVelocity.z/10D + 0.02 - Math.random()*0.04;
			this.prevMotion.setTo(motion);
		}
		
		this.definition = definition;
		this.boundingBox = new BoundingBox(position, getSize()/2D, getSize()/2D, getSize()/2D);
		this.maxAge = generateMaxAge();
		if(definition.color != null){
			if(definition.toColor != null){
				this.startColor = Color.decode(definition.color);
				this.endColor = Color.decode(definition.toColor);
				this.staticColor = null;
			}else{
				this.startColor = null;
				this.endColor = null;
				this.staticColor = Color.decode(definition.color);	
			}
		}else{
			this.startColor = null;
			this.endColor = null;
			this.staticColor = Color.decode("#FFFFFF"); 
		}
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//Set movement.
			if(definition.movementVelocity != null){
				motion.add(definition.movementVelocity);
				if(motion.x > definition.terminalVelocity.x){
					motion.x = definition.terminalVelocity.x;
				}
				if(motion.x < -definition.terminalVelocity.x){
					motion.x = -definition.terminalVelocity.x;
				}
				if(motion.y > definition.terminalVelocity.y){
					motion.y = definition.terminalVelocity.y;
				}
				if(motion.y < -definition.terminalVelocity.y){
					motion.y = -definition.terminalVelocity.y;
				}
				if(motion.z > definition.terminalVelocity.z){
					motion.z = definition.terminalVelocity.z;
				}
				if(motion.z < -definition.terminalVelocity.z){
					motion.z = -definition.terminalVelocity.z;
				}
			}else{
				switch(definition.type){
					case SMOKE: {
						//Update the motions to make the smoke float up.
						motion.x *= 0.9;
						motion.y += 0.004;
						motion.z *= 0.9;
						break;
					}
					case FLAME: {
						//Flame just slowly drifts in the direction it was going.
						motion.multiply(0.96);
						break;
					}
					case DRIP: {
						//Keep moving until we touch a block, then stop.
						if(!touchingBlocks){
							motion.multiply(0.96).add(0D, -0.06D, 0D);
						}else{
							motion.multiply(0.0);
						}
						break;
					}
					case BUBBLE: {
						//Bubbles float up until they break the surface of the water, then they pop.
						if(!world.isBlockLiquid(position)){
							remove();
						}else{
							motion.multiply(0.85).add(0, 0.002D, 0);
						}
						break;
					}
					case BREAK: {
						//Breaking just fall down quickly.
						if(!touchingBlocks){
							motion.multiply(0.98).add(0D, -0.04D, 0D);
						}else{
							motion.multiply(0.0);
						}
						break;
					}
				}
			}
			
			//Check collision movement.  If we hit a block, don't move.
			touchingBlocks = boundingBox.updateMovingCollisions(world, motion);
			if(touchingBlocks){
				motion.add(-boundingBox.currentCollisionDepth.x*Math.signum(motion.x), -boundingBox.currentCollisionDepth.y*Math.signum(motion.y), -boundingBox.currentCollisionDepth.z*Math.signum(motion.z));
			}
			position.add(motion);
			
			//Check age to see if we are on our last tick.
			if(++age == maxAge){
				remove();
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public boolean shouldSync(){
		return false;
	}
	
	@Override
	public boolean shouldSavePosition(){
		return false;
	}
	
	/**
	 *  Gets the max age of the particle.  This tries to use the definition's
	 *  maxAge, but will use Vanilla values if not set.  This should only be
	 *  called once, as the Vanilla values have a random element that means
	 *  this function will return different values on each call for them.
	 */
	private int generateMaxAge(){
		if(definition.duration != 0){
			return definition.duration;
		}else{
			switch(definition.type){
				case SMOKE: return (int)(8.0D / (Math.random() * 0.8D + 0.2D));
				case FLAME: return (int)(8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
				case DRIP: return  (int)(64.0D / (Math.random() * 0.8D + 0.2D));
				case BUBBLE: return (int)(8.0D / (Math.random() * 0.8D + 0.2D));
				case BREAK: return (int)(4.0D / (Math.random() * 0.9D + 0.1D));
			}
			//We'll never get here, but it makes the compiler happy.
			return 0;
		}
	}
	
	/**
	 *  Gets the current size of the particle.  This parameter
	 *  is used for the particle's bounding box, and does not need
	 *  to take the scale of the particle into account.
	 */
	public float getSize(){
		return definition.type.equals(ParticleType.DRIP) || definition.type.equals(ParticleType.BREAK) ? 0.1F : 0.2F;
	}
	
	/**
	 *  Gets the current red color value of the particle.  This parameter
	 *  is used to change the particle's color.  Normally only used on smoke.
	 */
	public float getRed(){
		if(staticColor != null){
			return staticColor.getRed()/255F;
		}else{
			return startColor.getRed() + (endColor.getRed() - startColor.getRed())*(float)age/maxAge/256F;
		}
	}
	
	/**
	 *  Gets the current green color value of the particle.  This parameter
	 *  is used to change the particle's color.  Normally only used on smoke.
	 */
	public float getGreen(){
		if(staticColor != null){
			return staticColor.getGreen()/255F;
		}else{
			return startColor.getGreen() + (endColor.getGreen() - startColor.getGreen())*(float)age/maxAge/256F;
		}
	}
	
	/**
	 *  Gets the current blue color value of the particle.  This parameter
	 *  is used to change the particle's color.  Normally only used on smoke.
	 */
	public float getBlue(){
		if(staticColor != null){
			return staticColor.getBlue()/255F;
		}else{
			return startColor.getBlue() + (endColor.getBlue() - startColor.getBlue())*(float)age/maxAge/256F;
		}
	}
	
	/**
	 *  Gets the current alpha value of the particle.  This parameter
	 *  is used to make the particle translucent.
	 */
	public float getAlpha(){
		if(definition.transparency != 0){
			if(definition.toTransparency != 0){
				return definition.transparency + (definition.toTransparency - definition.transparency)*age/maxAge;
			}else{
				return definition.transparency;
			}
		}else{
			return 1.0F;
		}
	}
	
	/**
	 *  Gets the current scale of the particle.
	 *  This is for rendering, and does not reflect particle bounds.
	 */
	public float getScale(float partialTicks){
		if(definition.scale != 0){
			if(definition.toScale != 0){
				return definition.scale + (definition.toScale - definition.scale)*age/maxAge;	
			}else{
				return definition.scale;
			}
		}else{
			switch(definition.type){
				case FLAME:  return (float) (1.0F - Math.pow((age + partialTicks)/maxAge, 2)/2F);
				case DRIP: return touchingBlocks ? 3.0F : 1.0F;
				default: return 1.0F;
			}
		}
	}
}
