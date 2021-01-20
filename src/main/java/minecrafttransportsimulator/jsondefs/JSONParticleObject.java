package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONParticleObject{
	@JSONRequired
	@JSONDescription("A string describing which type of particle to use. Currently this can be \"smoke\" or \"flame\".")
	public String type;
	@JSONDescription("A string in hexadecimal format representing the particle's color")
	public String color;
	@JSONDescription("Like above, but tells the particle to gradually change from its initial color to this value.\nDefaults to be the same as the initial color. NOT valid for engines")
	public String toColor;
	@JSONDescription("A number between 0.0 and 1.0 describing how transparent the particle should be")
	public float transparency;
	@JSONDescription("Like above, but tells the particle to gradually change from its initial transparency to this value.\nDefaults to 0.0, so it will fade to nothing if not set. NOT valid for engines.")
	public float toTransparency;
	@JSONDescription("How big to spawn each particle. A value of 1.0 will result in 1 pixel of the particle texture per 1 pixel in-game.")
	public float scale;
	@JSONDescription("Like above, but tells the particle to gradually change from its initial scale to this value.\nDefaults to 0.0, so it will shrink to nothing if this is not set. Not valid for engines.")
	public float toScale;
	@JSONDescription("The position where this particle should be spawned relative to the vehicle or bullet.")
	public Point3d pos;
	@Deprecated
	public float velocity;
	public Point3d velocityVector;
	public int quantity;
	public int duration;
}
