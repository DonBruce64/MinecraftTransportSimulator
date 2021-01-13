package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONText{
	@JSONRequired
	public Point3d pos;
	@JSONRequired
    public Point3d rot;
	public float scale;
	public String fieldName;
    public String defaultText;
    public int maxLength;
    public int wrapWidth;
    @JSONRequired
    public String color;
    public String attachedTo;
	
	public boolean lightsUp;
	public int renderPosition;
	public boolean autoScale;
	public boolean colorInherited;
}
