package minecrafttransportsimulator.jsondefs;

import java.util.List;

public class JSONRoadComponent extends AJSONModelProvider<JSONRoadComponent.RoadGeneral>{

    public class RoadGeneral extends AJSONModelProvider<JSONRoadComponent.RoadGeneral>.General{
    	public String type;
    	public int numberLanes;
    	public float width;
    	public int collisionHeight;
    	public float centerOffset;
    }
}