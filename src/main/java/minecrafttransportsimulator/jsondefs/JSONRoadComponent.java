package minecrafttransportsimulator.jsondefs;

public class JSONRoadComponent extends AJSONMultiModelProvider<JSONRoadComponent.RoadGeneral>{

    public class RoadGeneral extends AJSONMultiModelProvider<JSONRoadComponent.RoadGeneral>.General{
    	public String type;
    	
    	public float[] laneOffsets;
    	public float[] markingOffsets;
    	public float borderOffset;
    	public int collisionHeight;
    	
    	public boolean onlyAtJoints;
    }
}