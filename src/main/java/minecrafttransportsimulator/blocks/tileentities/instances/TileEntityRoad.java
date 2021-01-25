package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityMultiblock;
import minecrafttransportsimulator.blocks.tileentities.components.RoadClickData;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONLaneSector;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.instances.RenderRoad;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Road tile entity.  Contains the definition so we know how
 * to render this in the TESR call, as well as stores the "fake"
 * block positions of blocks that make up this road segment.
 * Optionally stores the next and prior road in the segment, if one exists.
 * This can be used to create smooth road segments.
 * Note that while the number of connection points is finite, there may be multiple
 * curves connected to and from each point.  However, each road segment can only have
 * one curve from any one lane connection.  Therefore, to make junctions you will have
 * multiple segments connected to the same point, but each of those segments will only be
 * connected to that junction and nowhere else.
 * Also note that all points and positions are relative.  This is done for simpler
 * math operations.  Should the world-based point be needed, simply offset any relative
 * point by the position of this TE.
 *
 * @author don_bruce
 */
public class TileEntityRoad extends ATileEntityMultiblock<JSONRoadComponent>{
	//Static variables based on core definition.
	public final BoundingBox boundingBox;
	public BezierCurve dynamicCurve;
	public final List<RoadLane> lanes;

	//Dynamic variables based on states.
	public final Map<RoadComponent, ItemRoadComponent> components = new HashMap<RoadComponent, ItemRoadComponent>();
	
	public TileEntityRoad(WrapperWorld world, Point3i position, WrapperNBT data){
		super(world, position, data);
		//Set the bounding box.
		this.boundingBox = new BoundingBox(new Point3d(0, (definition.general.collisionHeight - 16)/16D/2D, 0), 0.5D, definition.general.collisionHeight/16D/2D, 0.5D);
		
		//Load components back in.  Our core component will always be our definition.
		for(RoadComponent componentType : RoadComponent.values()){
			String packID = data.getString("packID" + componentType.ordinal());
			if(!packID.isEmpty()){
				String systemName = data.getString("systemName" + componentType.ordinal());
				ItemRoadComponent newComponent = PackParserSystem.getItem(packID, systemName);
				components.put(componentType, newComponent);
			}
		}
		components.put(RoadComponent.CORE, (ItemRoadComponent) item);
		
		//Load curve and lane data.  We may not have this yet if we're in the process of creating a new road.
		this.lanes = new ArrayList<RoadLane>();
		Point3d startingOffset = data.getPoint3d("startingOffset");
		Point3d endingOffset = data.getPoint3d("endingOffset");
		if(!endingOffset.isZero()){
			this.dynamicCurve = new BezierCurve(startingOffset, endingOffset, (float) data.getDouble("startingRotation"), (float) data.getDouble("endingRotation"));
		}
		
		//Don't generate lanes for inactive roads.
		if(isActive()){
			generateLanes(data);
		}
	}
	
    @Override
    public void setActive(boolean active){
    	super.setActive(active);
    	if(active){
	    	generateLanes(null);
	    }
    }
	
	@Override
	public List<AItemPack<JSONRoadComponent>> getDrops(){
		List<AItemPack<JSONRoadComponent>> drops = new ArrayList<AItemPack<JSONRoadComponent>>();
		for(RoadComponent componentType : RoadComponent.values()){
			if(components.containsKey(componentType)){
				drops.add(components.get(componentType));
			}
		}
		return drops;
	}
	
	/**
	 *  Helper method to get information on what was clicked.
	 *  Takes the player's rotation into account, as well as the block they clicked.
	 */
	public RoadClickData getClickData(Point3i blockOffsetClicked, boolean curveStart){
		boolean clickedStart = blockOffsetClicked.isZero() || collisionBlockOffsets.indexOf(blockOffsetClicked) < collisionBlockOffsets.size()/2;
		JSONLaneSector closestSector = null;
		if(!definition.general.isDynamic){
			double closestSectorDistance = Double.MAX_VALUE;
			for(RoadLane lane : lanes){
				//Only check start points.  End points are for other sectors.
				double distanceToSectorStart = lane.curves.get(0).startPos.distanceTo(blockOffsetClicked);
				if(distanceToSectorStart < closestSectorDistance){
					closestSectorDistance = distanceToSectorStart;
					closestSector = definition.general.sectors.get(lane.sectorNumber);
				}
			}
		}
		return new RoadClickData(this, closestSector, clickedStart, curveStart);
	}
	
	/**
	 *  Helper method to populate the lanes for this road.  This depends on if we are
	 *  a static or dynamic road.  Data is passed-in, but may be null if we're generating
	 *  lanes for the first time.
	 */
	public void generateLanes(WrapperNBT data){
		if(definition.general.isDynamic){
			for(int i=0; i<definition.general.laneOffsets.length; ++i){
				lanes.add(new RoadLane(this, 0, lanes.size(), data != null ? data.getData("lane" + lanes.size()) : null));
			}
		}else{
			for(int i=0; i<definition.general.sectors.size(); ++i){
				for(int j=0; j<definition.general.sectors.get(i).lanes.size(); ++j){
					lanes.add(new RoadLane(this, i, lanes.size(), data != null ? data.getData("lane" + lanes.size()) : null));
				}
			}
		}
	}
	
	/**
	 *  Helper method to spawn collision boxes for this road.  Returns true and makes
	 *  this road non-holographic if the boxes could be spawned.  False if there are
	 *  blocking blocks.  OP and creative-mode players override blocking block checks.
	 *  Road width is considered to extend to the left and right border, minus 1/2 a block.
	 */
	@Override
	protected Map<Point3i, Integer> generateCollisionPoints(){
		collisionBlockOffsets.clear();
		collidingBlockOffsets.clear();
		Map<Point3i, Integer> collisionHeightMap = new HashMap<Point3i, Integer>();
		if(definition.general.isDynamic){
			//Get all the points that make up our collision points for our dynamic curve.
			//If we find any colliding points, note them.
			Point3d testOffset = new Point3d();
			Point3d testRotation = new Point3d();
			float segmentDelta = (float) (definition.general.borderOffset/(Math.floor(definition.general.borderOffset) + 1));
			for(float f=0; f<dynamicCurve.pathLength; f+=0.1){
				for(float offset=0; offset < definition.general.borderOffset; offset += segmentDelta){
					dynamicCurve.setPointToRotationAt(testRotation, f);
					//We only want yaw for block placement.
					testRotation.x = 0;
					testRotation.z = 0;
					testOffset.set(offset, 0, 0).rotateCoarse(testRotation).add(0, definition.general.collisionHeight/16F, 0);
					dynamicCurve.offsetPointByPositionAt(testOffset, f);
					Point3i testPoint = new Point3i((int) testOffset.x, (int) Math.floor(testOffset.y), (int) testOffset.z);
					
					//If we don't have a block in this position, check if we need one.
					if(!testPoint.isZero() && !collisionBlockOffsets.contains(testPoint) && !collidingBlockOffsets.contains(testPoint)){
						//Offset the point to the global cordinate space, get the block, and offset back.
						testPoint.add(position);
						if(world.isAir(testPoint)){
							//Need a collision box here.
							testPoint.subtract(position);
							int collisionBoxIndex = (int) ((testOffset.y - testPoint.y)*16);
							collisionBlockOffsets.add(testPoint);
							
							collisionHeightMap.put(testPoint, collisionBoxIndex);
						}else if(!(world.getBlock(testPoint) instanceof BlockCollision)){
							//Some block is blocking us that's not part of a road.  Flag it.
							testPoint.subtract(position);
							collidingBlockOffsets.add(testPoint);
						}
					}
				}
			}
			return collisionHeightMap;
		}else{
			return super.generateCollisionPoints();
		}
	}
	
	@Override
	public RenderRoad getRenderer(){
		return new RenderRoad();
	}
	
	@Override
    public void save(WrapperNBT data){
		super.save(data);
		//Save all components.
		for(Entry<RoadComponent, ItemRoadComponent> connectedObjectEntry : components.entrySet()){
			data.setString("packID" + connectedObjectEntry.getKey().ordinal(), connectedObjectEntry.getValue().definition.packID);
			data.setString("systemName" + connectedObjectEntry.getKey().ordinal(), connectedObjectEntry.getValue().definition.systemName);
		}
		
		//Save curve data.
		if(dynamicCurve != null){
			data.setPoint3d("startingOffset", dynamicCurve.startPos);
			data.setPoint3d("endingOffset", dynamicCurve.endPos);
			data.setDouble("startingRotation", dynamicCurve.startAngle);
			data.setDouble("endingRotation", dynamicCurve.endAngle);
		}
		
		//Save lane data.
		for(int laneNumber=0; laneNumber < lanes.size(); ++laneNumber){
			RoadLane lane = lanes.get(laneNumber);
			WrapperNBT laneData = new WrapperNBT();
			lane.save(laneData);
			if(definition.general.isDynamic){
				data.setData("lane" + laneNumber, laneData);
			}else{
				data.setData("lane" + laneNumber, laneData);
			}
		}
    }
	
	/**
	 *  Enums for part-specific stuff.
	 */
	public static enum RoadComponent{
		CORE,
		LEFT_MARKING,
		RIGHT_MARKING,
		CENTER_MARKING,
		LEFT_BORDER,
		RIGHT_BORDER,
		UNDERLAYMENT,
		SUPPORT;
	}
}
