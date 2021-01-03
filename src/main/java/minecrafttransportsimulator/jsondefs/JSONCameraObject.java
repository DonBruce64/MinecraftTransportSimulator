package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;

public class JSONCameraObject{
	public Point3d pos;
	public Point3d rot;
	public float fovOverride;
	public String overlay;
	public List<JSONAnimationDefinition> animations;
}
