package minecrafttransportsimulator.rendering.components;

import java.awt.Color;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart.ParticleObject;
import minecrafttransportsimulator.mcinterface.IInterfaceRender;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;

/**Class that is the base of all particles.
 * Particles may be spawned by calling {@link IInterfaceRender#spawnParticle(AParticle)}
 *
 * @author don_bruce
 */
public abstract class AParticle{
	
	public final IWrapperWorld world;
	public final Point3d position;
	public final Point3d motion;
	public final BoundingBox box;
	public int maxAge;
	public final ParticleObject particleObject;

	public float red;
	public float green;
	public float blue;
	public float alpha;
	public boolean touchingBlocks;
	public float scale;
	public int age;
	public boolean isValid;
	
	public float deltaRed;
	public float deltaGreen;
	public float deltaBlue;
	public float deltaAlpha;
	public float deltaScale;
	
	public AParticle(IWrapperWorld world, Point3d position, Point3d motion){
		this(world, position, motion, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
	}

	public AParticle(IWrapperWorld world, Point3d position, Point3d motion, ParticleObject particleObject){
		this.world = world;
		this.position = position;
		this.motion = motion;
		this.box = new BoundingBox(position, getSize()/2D, getSize()/2D, getSize()/2D);

		Color color = particleObject.color != null ? Color.decode(particleObject.color) : Color.decode("#FFFFFF");
		this.red = color.getRed()/255F;
		this.green = color.getGreen()/255F;
		this.blue = color.getBlue()/255F;
		this.alpha = particleObject.transparency;
		this.scale = particleObject.scale;
		this.particleObject = particleObject;
		this.maxAge = generateMaxAge();
		this.setDeltas();
		this.isValid = true;
	}
	
	public AParticle(IWrapperWorld world, Point3d position, Point3d motion, float red, float green, float blue, float alpha, float scale){
		this.world = world;
		this.position = position;
		this.motion = motion;
		this.box = new BoundingBox(position, getSize()/2D, getSize()/2D, getSize()/2D);
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.alpha = alpha;
		this.scale = scale;
		this.particleObject = null;
		this.maxAge = generateMaxAge();
		this.isValid = true;
	}
	
	/**
	 *  Called to update this particle's states.
	 */
	public void update(){
		if(!isValid) {
			return;
		}
		if(collidesWithBlocks()){
			touchingBlocks = box.updateMovingCollisions(world, motion);
			if(touchingBlocks){
				motion.add(-box.currentCollisionDepth.x*Math.signum(motion.x), -box.currentCollisionDepth.y*Math.signum(motion.y), -box.currentCollisionDepth.z*Math.signum(motion.z));
			}
		}
		position.add(motion);
		
		//Change color, alpha, and scale if necessary.
		//This is proportional to its point in its life cycle.
		this.red += this.deltaRed;
		this.green += this.deltaGreen;
		this.blue += this.deltaBlue;
		this.alpha += this.deltaAlpha;
		this.scale += this.deltaScale;
		
		if(++age == maxAge) {
			isValid = false;
		}
	}
	
	/**
	 *  Sets values for incrementally changing color, transparency, and scale later on.
	 *  It takes the desired end-states of these values, and calculates how much the
	 *  values need to change each tick to reach the given value at maxAge.
	 */
	public void setDeltas() {
		//Establish how much to change each tick.
		float deltaAmount = 1f/(this.maxAge - this.age);
		
		Color toColor = particleObject.toColor != null ? Color.decode(particleObject.toColor) : Color.decode(particleObject.color);
		this.deltaRed = (toColor.getRed()/255F - this.red) * deltaAmount;
		this.deltaGreen = (toColor.getGreen()/255F - this.green) * deltaAmount;
		this.deltaBlue = (toColor.getBlue()/255F - this.blue) * deltaAmount;
			
		this.deltaAlpha = (particleObject.toTransparency - this.alpha) * deltaAmount;
		this.deltaScale = (particleObject.toScale - this.scale) * deltaAmount;
	};
	
	/**
	 *  Returns true if this particle should be prevented from moving through blocks.
	 *  This affects the particle's motion, so only use this if you don't mind the motion
	 *  being automatically adjusted to handle block collisions.  Also note that this only
	 *  can check in 1-block step increments, so if the particle is moving faster than
	 *  1 block per update tick, it won't collide with some blocks.
	 */
	public boolean collidesWithBlocks(){
		return true;
	}
	
	/**
	 *  Gets the current scale of the particle.
	 *  This is for rendering, and does not reflect particle bounds.
	 */
	public abstract float getScale(float partialTicks);
	
	/**
	 *  Gets the current size of the particle.  This parameter
	 *  is used for the particle's bounding box, and does not need
	 *  to take the scale of the particle into account.
	 */
	public float getSize(){
		return 0.2F;
	}
	
	/**
	 *  Generates the max age for this particle.  This value is used only once on construction
	 *  to initialize the {@link #maxAge} parameter.  This parameter is how many ticks the particle will
	 *  render and move until it is removed from the world.
	 */
	protected abstract int generateMaxAge();
	
	/**
	 *  Returns the packed UV index for this texture.
	 *  The packed UV index is a single integer divided by 16, with the
	 *  quotient of the value being the U value, and the remainder
	 *  being the V value.  So for example, a texture index of 22
	 *  would have a quotient of 1, and a remainder of 6, so would have
	 *  a UV of 1,6.  If this particle does not have a texture index and
	 *  uses a custom model, return -1.
	 */
	public abstract int getTextureIndex();
	
	/**
	 *  Return true if this particle should be lit up at night.  This is normally
	 *  not the case, but may be if the particle needs to be bright.
	 */
	public boolean isBright(){
		return false;
	}
	
	/**
	 *  Renders this particle.  Normally this is automatic and handled by the 2D texture
	 *  rendering system based on the texture index returned by {@link #getTextureIndex()}.
	 *  However, if that function returns -1, this method will be called instead.
	 *  Note that you are required to bind the texture you will be using here in this case;
	 *  it will not be pre-bound for you.
	 */
	public void render(float partialTicks){}
}
