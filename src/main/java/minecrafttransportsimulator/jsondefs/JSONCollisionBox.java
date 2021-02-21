package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONCollisionBox{
	@JSONRequired
	public Point3d pos;
    public float width;
    public float height;
    public boolean isInterior;
    public boolean collidesWithLiquids;
    public float armorThickness;
}
