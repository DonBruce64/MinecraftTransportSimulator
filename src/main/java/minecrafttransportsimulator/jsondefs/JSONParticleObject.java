package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONParticleObject{
	@JSONRequired
	@JSONDescription("Which type of particle to use.")
	public ParticleComponentType type;
	
	@JSONDescription("A string in hexadecimal format representing the particle's color.  Only valid for smoke.")
	public String color;
	
	@JSONDescription("Like above, but tells the particle to gradually change from its initial color to this value.\nDefaults to be the same as the initial color. NOT valid for engines")
	public String toColor;
	
	@JSONDescription("A number between 0.0 and 1.0 describing how transparent the particle should be.  Only valid for smoke.")
	public float transparency;
	
	@JSONDescription("Like above, but tells the particle to gradually change from its initial transparency to this value.\nDefaults to 0.0, so it will fade to nothing if not set. NOT valid for engines.")
	public float toTransparency;
	
	@JSONDescription("How big to spawn each particle. A value of 1.0 will result in 1 pixel of the particle texture per 1 pixel in-game.")
	public float scale;
	
	@JSONDescription("Like above, but tells the particle to gradually change from its initial scale to this value.\nDefaults to 0.0, so it will shrink to nothing if this is not set. Not valid for engines.")
	public float toScale;
	
	@JSONDescription("The position where this particle should be spawned relative to the vehicle or bullet.  May be left out if the particle should spawn at the same position.")
	public Point3d pos;
	
	@JSONDescription("The velocity of the particle, where +Z is straight ahead relative to the vehicle, gun, or bullet that is producing it.")
	public Point3d velocityVector;
	
	@JSONDescription("Integer representing how many of this particle to spawn at a time. Defaults to 1.")
	public int quantity;
	
	@JSONDescription("How long, in ticks, the smoke particle should remain. Normally this is around 13, but it can be longer for things like aerobatic smoke trails. Not valid for engines.")
	public int duration;
	
	@Deprecated
	public float velocity;
	
	public static enum ParticleComponentType{
		@JSONDescription("The standard smoke particle.")
		SMOKE,
		@JSONDescription("The standard (torch) flame particle.")
		FLAME;
	}
}
