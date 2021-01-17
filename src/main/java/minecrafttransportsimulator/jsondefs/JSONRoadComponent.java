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
    	public List<JSONLaneSector> sectors;
    	public List<JSONRoadCollisionArea> collisionAreas;
    }
    
    public class JSONLaneSector{
    	public Point3d sectorStartPos;
    	public float sectorStartAngle;
    	public float borderOffset;
    	public List<JSONLaneSectorPointSet> lanes;
    }
    
    public class JSONLaneSectorPointSet{
    	public Point3d startPoint;
    	public List<JSONLaneSectorEndPoint> endPoints;
    }
    
    public class JSONLaneSectorEndPoint{
    	public Point3d pos;
    	public float angle;
    }
    
    public class JSONRoadCollisionArea{
    	public Point3d firstCorner;
    	public Point3d secondCorner;
    	public int collisionHeight;
    }
}