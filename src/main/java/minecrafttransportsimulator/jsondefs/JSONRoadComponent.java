package minecrafttransportsimulator.jsondefs;

import java.util.List;

public class JSONRoadComponent extends AJSONModelProvider<JSONRoadComponent.RoadGeneral>{

    public class RoadGeneral extends AJSONModelProvider<JSONRoadComponent.RoadGeneral>.General{
    	public String type;
    	public float firstLaneOffset;
    	public float laneWidth;
    	public int numberLanes;
    	public int collisionHeight;
    }
}