package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@Deprecated
public class JSONDoor{
	@JSONRequired
	@Deprecated
	public String name;
	
    @JSONRequired
    @Deprecated
    public Point3d closedPos;
    
	@JSONRequired
	@Deprecated
	public Point3d openPos;
	
	@Deprecated
    public float width;
	
	@Deprecated
    public float height;
	
	@Deprecated
    public float armorThickness;
	
	@Deprecated
    public boolean closedByDefault;
	
	@Deprecated
    public boolean closeOnMovement;
	
	@Deprecated
    public boolean activateOnSeated;
	
	@Deprecated
    public boolean ignoresClicks;
}
