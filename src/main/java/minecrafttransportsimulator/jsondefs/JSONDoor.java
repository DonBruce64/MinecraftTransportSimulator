package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONDoor{
	@JSONRequired
	public String name;
    @JSONRequired
    public Point3d closedPos;
	@JSONRequired
	public Point3d openPos;
    public float width;
    public float height;
    public boolean closedByDefault;
    public boolean closeOnMovement;
    public boolean activateOnSeated;
    public boolean ignoresClicks;
}
