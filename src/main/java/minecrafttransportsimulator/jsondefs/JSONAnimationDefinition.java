package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3d;

public class JSONAnimationDefinition{
	public String animationType;
	public String variable;
	public Point3d centerPoint;
	public Point3d axis;
	public float offset;
	public boolean addPriorOffset;
	public float clampMin;
	public float clampMax;
	public boolean absolute;
	public int duration;
	public int forwardsDelay;
	public int reverseDelay;
	public String forwardsStartSound;
	public String forwardsEndSound;
	public String reverseStartSound;
	public String reverseEndSound;
}
