package minecrafttransportsimulator.jsondefs;

import java.util.List;

public class JSONRoadComponent extends AJSONModelProvider<JSONRoadComponent.RoadGeneral>{

    public class RoadGeneral extends AJSONModelProvider<JSONRoadComponent.RoadGeneral>.General{
    	public String type;
    	
    	public float[] laneOffsets;
    	public float[] markingOffsets;
    	public float[] borderOffsets;
    	public int collisionHeight;
    	
    	public boolean onlyAtJoints;
    }
}