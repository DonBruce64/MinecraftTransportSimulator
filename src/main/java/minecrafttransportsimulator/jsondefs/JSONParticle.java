package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONParticle{
	@JSONRequired
	@JSONDescription("Which type of particle to use.")
	public ParticleType type;
	
	@JSONDescription("A listing of animation objects for determining if this particle should spawn.  Particles will only spawn when they first become active, unless spawnEveryTick is set.")
	public List<JSONAnimationDefinition> activeAnimations;
	
	@JSONDescription("Foces this particle to spawn every tick it is active.  Useful for constant particle flows, like smoke.")
	public boolean spawnEveryTick;
	
	@JSONDescription("A string in hexadecimal format representing the particle's color.  Defaults to white if not set, which essentially does no color modification.")
	public String color;
	
	@JSONDescription("Like above, but tells the particle to gradually change from its initial color to this value.  Defaults to be the same as the initial color.")
	public String toColor;
	
	@JSONDescription("A number between 0.0 and 1.0 describing how transparent the particle should be.  If both this and toTransparency are not set, they are assumed to be 1.0 for both and no transparency changes will be performed.")
	public float transparency;
	
	@JSONDescription("Like above, but tells the particle to gradually change from its initial transparency to this value.  If transparency is set and non-zero, then this defaults to 0.0.  Othwerise, it is used as-is.")
	public float toTransparency;
	
	@JSONDescription("How big to spawn each particle.  A value of 1.0 will result in 1 pixel of the particle texture per 1 pixel in-game.  This is the default if this is not set.")
	public float scale;
	
	@JSONDescription("Like above, but tells the particle to gradually change from its initial scale to this value.  Defaults to 1.0 if this and scale are not set.")
	public float toScale;
	
	@JSONDescription("The position where this particle should be spawned relative to the spawning object.  May be left out if the particle should spawn at the same position.")
	public Point3d pos;
	
	@JSONDescription("The initial velocity of the particle, where +Z is straight ahead relative to the thing that is producing it.  May be omitted to make a particle that doesn't spawn with any initial velocity except the velocity of the object spawning it.")
	public Point3d initialVelocity;
	
	@JSONDescription("The velocity to apply every tick to the particle.  This can be used to make smoke float up, oil drip down, etc.  If not set, the default particle velocity is used.")
	public Point3d movementVelocity;
	
	@JSONDescription("The max velocity this particle can have in any axis.  Used to prevent particles from going to fast if they move a long way.")
	public Point3d terminalVelocity;
	
	@JSONDescription("How many of this particle to spawn at a time. Defaults to 1.")
	public int quantity;
	
	@JSONDescription("How long, in ticks, the particle should remain.  If not set on a defined type, the age will be auto-calculated the same way it would be for a Vanilla particle.  Defaults to 200 on any other types.")
	public int duration;
	
	@Deprecated
	public Point3d velocityVector;
	
	public static enum ParticleType{
		@JSONDescription("The standard smoke particle.")
		SMOKE,
		@JSONDescription("The standard (torch) flame particle.")
		FLAME,
		@JSONDescription("The standard water/drip particle.")
		DRIP,
		@JSONDescription("The standard bubble particle.")
		BUBBLE,
		@JSONDescription("The standard block breakage particle. The block texture to use will always be the block below this particle when first spawned.")
		BREAK;
	}
}
