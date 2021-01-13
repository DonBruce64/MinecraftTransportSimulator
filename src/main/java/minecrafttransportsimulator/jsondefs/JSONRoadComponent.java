package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;

public class JSONRoadComponent extends AJSONMultiModelProvider<JSONRoadComponent.RoadGeneral>{

    public class RoadGeneral extends AJSONMultiModelProvider<JSONRoadComponent.RoadGeneral>.General{
    	//Common variables.
    	public String type;
    	public boolean isDynamic;
    	
    	//Dynamic variables.
    	public float[] laneOffsets;
    	public float borderOffset;
    	public int collisionHeight;
    	
    	//Static variables.
    	public List<JSONLanePointSet> lanePoints;
    	public List<JSONRoadCollisionArea> collisionAreas;
    }
    
    public class JSONLanePointSet{
    	public Point3d startPos;
    	public Point3d endPos;
    	public float startAngle;
    	public float endAngle;
    }
    
    public class JSONRoadCollisionArea{
    	public Point3d firstCorner;
    	public Point3d secondCorner;
    	public int collisionHeight;
    }
}