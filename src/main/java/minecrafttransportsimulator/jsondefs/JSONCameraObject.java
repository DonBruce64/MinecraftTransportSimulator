package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONCameraObject{
	@JSONRequired
	public Point3d pos;
	@JSONRequired
	public Point3d rot;
	public float fovOverride;
	public String overlay;
	public List<JSONAnimationDefinition> animations;
}
