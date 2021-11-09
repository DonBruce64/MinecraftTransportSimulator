package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONRoadComponent extends AJSONMultiModelProvider{

	@JSONDescription("Road-generic properties.")
	public JSONRoadGeneric road;
	
    public class JSONRoadGeneric{
    	//Common variables.
    	@JSONDescription("The type of this road component.  This defines its properties.")
    	public RoadComponent type;
    	
    	@JSONRequired(dependentField="type", dependentValues={"CORE_DYNAMIC"})
    	@JSONDescription("The offsets for the lanes for this road if this is a dynamic core component.  0 starts at X=0.")
    	public float[] laneOffsets;
    	
    	@JSONRequired(dependentField="type", dependentValues={"CORE_DYNAMIC"})
    	@JSONDescription("The offsets for the opposite side of the road if this is a dynamic core component.  Essentially the width of the road.")
    	public float borderOffset;
    	
    	@JSONRequired(dependentField="type", dependentValues={"CORE_DYNAMIC"})
    	@JSONDescription("How long each segment in this road is.  This allows for variable-length repeating model segments.")
    	public float segmentLength;
    	
    	@JSONRequired
    	@JSONDescription("How high collision should be for this dynamic core component, in pixels.  Normally a low value.")
    	public int collisionHeight;
    	
    	@JSONRequired(dependentField="type", dependentValues={"CORE_STATIC"})
    	@JSONDescription("A list of lane sectors that define the paths for this road.  These contain a start position, angle, and lanes.  Each sector is basically an intersection, with a 4-way intersection having 4 sectors.  The nproperties in a sector is used to determine what lanes can connect to the sector, and where these connections are.")
    	public List<JSONLaneSector> sectors;
    	
    	@JSONRequired(dependentField="type", dependentValues={"CORE_STATIC"})
    	@JSONDescription("A list of collision areas.  This allows for collision to be applied in a multi-block format for this road.  Normally just a single square entry, but may extend in the +Y direction for complex road components like bridges.  Think of how you seelect ares with the World Edit mod.")
    	public List<JSONRoadCollisionArea> collisionAreas;
    }
    
    public class JSONLaneSector{
    	@JSONRequired
    	@JSONDescription("The start position for this sector.  This should be the right-most side.  For example, on a 4-lane intersection this would be 0,0,0 for the south-facing sector.")
    	public Point3d sectorStartPos;
    	
    	@JSONDescription("The start angle for this sector.  Roads will normally be considered to be heading south, but this rotates them to whatever direction this sector is facing.")
    	public float sectorStartAngle;
    	
    	@JSONDescription("How far from the start position the border of this sector is.  Similar to dynamic roads, this is used to calculate the total road width.")
    	public float borderOffset;
    	
    	@JSONRequired
    	@JSONDescription("A set of lane points that make up the paths for this sector.")
    	public List<JSONLaneSectorPointSet> lanes;
    }
    
    public class JSONLaneSectorPointSet{
    	@JSONRequired
    	@JSONDescription("The starting point for this lane.  Note that vehicles arriving at junctions will only transition to the next road segment if there's a start position for it.  This allows for merge areas where two lanes go into one, as well as one-way roads.")
    	public Point3d startPoint;
    	
    	@JSONRequired
    	@JSONDescription("A list of end-points for this lane.  This may be, and likely will, be the same as the start points for the lanes from other sectors.")
    	public List<JSONLaneSectorEndPoint> endPoints;
    }
    
    public class JSONLaneSectorEndPoint{
    	@JSONRequired
    	@JSONDescription("The ending position for this sector-point.")
    	public Point3d pos;
    	
    	@JSONDescription("The ending rotation for this sector-point.")
    	public float angle;
    }
    
    public class JSONRoadCollisionArea{
    	@JSONRequired
    	@JSONDescription("The first corner point that defines this collision area.")
    	public Point3d firstCorner;
    	
    	@JSONRequired
    	@JSONDescription("The second corner point that defines this collision area.  The Y-position MUST be the same as the first point!")
    	public Point3d secondCorner;
    	
    	@JSONDescription("The height of the collision for this area, in pixels.  Not that this may NOT exceed 15 and go above the height of a block.  If you want collision that's over 1-block high, use another collision area with the two points at a higher Y position.")
    	public int collisionHeight;
    }
}