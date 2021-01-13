package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONAnimationDefinition{
	@JSONRequired
	public String animationType;
	@JSONRequired
	public String variable;
	@JSONRequired(dependentField="animationType", dependentValues={"rotation"})
	public Point3d centerPoint;
	@JSONRequired(dependentField="animationType", dependentValues={"rotation", "translation"})
	public Point3d axis;
	public float offset;
	public boolean addPriorOffset;
	@JSONRequired(dependentField="animationType", dependentValues={"visibility"})
	public float clampMin;
	@JSONRequired(dependentField="animationType", dependentValues={"visibility"})
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
