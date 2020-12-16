package minecrafttransportsimulator.jsondefs;

public class JSONRoadComponent extends AJSONModelProvider<JSONRoadComponent.RoadGeneral>{

    public class RoadGeneral extends AJSONModelProvider<JSONRoadComponent.RoadGeneral>.General{
    	public String type;
    	
    	public float[] laneOffsets;
    	public float[] markingOffsets;
    	public float leftBorderOffset;
    	public float rightBorderOffset;
    	public int collisionHeight;
    	
    	public boolean onlyAtJoints;
    }
}