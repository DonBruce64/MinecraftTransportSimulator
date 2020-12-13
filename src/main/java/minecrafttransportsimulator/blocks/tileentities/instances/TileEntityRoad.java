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
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
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
public class TileEntityRoad extends ATileEntityBase<JSONRoadComponent>{
	//Static variables based on core definition.
	public final BoundingBox boundingBox;
	public final List<Point3i> collisionBlockOffsets;
	public final List<RoadLane> lanes;

	//Dynamic variables based on states.
	public final Map<RoadComponent, ItemRoadComponent> components = new HashMap<RoadComponent, ItemRoadComponent>();
	public final List<Point3i> collidingBlockOffsets = new ArrayList<Point3i>();
	public boolean isHolographic;
	
	//Static constants.
	public static final int MAX_CURVE_CONNECTIONS = 3;
	public static final int MAX_SEGMENT_LENGTH = 32;
	
	public TileEntityRoad(IWrapperWorld world, Point3i position, IWrapperNBT data){
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
		
		//Load lane data.  We may not have this yet if we're in the process of creating a new road.
		this.lanes = new ArrayList<RoadLane>();
		float[] definitionOffsets = components.get(RoadComponent.CORE).definition.general.laneOffsets;
		for(int laneNumber=0; laneNumber < definitionOffsets.length; ++laneNumber){
			IWrapperNBT laneData = data.getData("lane" + laneNumber);
			if(laneData != null){
				lanes.add(new RoadLane(laneData));
			}
		}
		
		//If we have points for collision due to use creating collision blocks, load them now.
		this.collisionBlockOffsets = data.getPoints("collisionBlockOffsets");
		
		//Get the holographic state.
		this.isHolographic = data.getBoolean("isHolographic");
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
	
	@Override
	public RenderRoad getRenderer(){
		return new RenderRoad();
	}
	
	@Override
    public void save(IWrapperNBT data){
		super.save(data);
		//Save all components.
		for(Entry<RoadComponent, ItemRoadComponent> connectedObjectEntry : components.entrySet()){
			data.setString("packID" + connectedObjectEntry.getKey().ordinal(), connectedObjectEntry.getValue().definition.packID);
			data.setString("systemName" + connectedObjectEntry.getKey().ordinal(), connectedObjectEntry.getValue().definition.systemName);
		}
		
		//Save lane data.
		for(int laneNumber=0; laneNumber < lanes.size(); ++laneNumber){
			RoadLane lane = lanes.get(laneNumber);
			IWrapperNBT laneData = MasterLoader.coreInterface.createNewTag();
			lane.save(laneData);
			data.setData("lane" + laneNumber, laneData);
		}
		
		//Save cure collision point data.
		data.setPoints("collisionBlockOffsets", collisionBlockOffsets);
		
		//Save holographic state.
		data.setBoolean("isHolographic", isHolographic);
    }
	
	/**
	 *  Helper class for containing lane data.
	 */
	public class RoadLane{
		public final Point3d startingOffset;
		public final Point3d endingOffset;
		public final BezierCurve curve;
		public RoadLaneConnection priorConnection;
		public RoadLaneConnection nextConnection;
		
		public RoadLane(Point3d startingOffset, Point3d endingOffset, double endingRotation){
			this.startingOffset = startingOffset;
			this.endingOffset = endingOffset.copy().subtract(startingOffset);
			this.curve = new BezierCurve(this.endingOffset, (float) rotation, (float) endingRotation);
		}
		
		public RoadLane(IWrapperNBT data){
			this.startingOffset = data.getPoint3d("startingOffset");
			this.endingOffset = data.getPoint3d("endingOffset");
			this.curve = new BezierCurve(endingOffset, (float) rotation, (float) data.getDouble("endingRotaton"));
			IWrapperNBT priorConnectionData = data.getData("priorConnection");
			if(priorConnectionData != null){
				priorConnection = new RoadLaneConnection(priorConnectionData);
			}
			IWrapperNBT nextConnectionData = data.getData("nextConnection");
			if(nextConnectionData != null){
				nextConnection = new RoadLaneConnection(nextConnectionData);
			}
		}
		
		public void connectToPrior(TileEntityRoad road, int laneNumber, boolean connectedToStart){
			priorConnection = new RoadLaneConnection(road.position, laneNumber, connectedToStart);
		}
		
		public void connectToNext(TileEntityRoad road, int laneNumber, boolean connectedToStart){
			nextConnection = new RoadLaneConnection(road.position, laneNumber, connectedToStart);
		}
		
		public void save(IWrapperNBT data){
			data.setPoint3d("startingOffset", startingOffset);
			data.setPoint3d("endingOffset", curve.endPos);
			data.setDouble("endingRotaton", curve.endAngle);
			if(priorConnection != null){
				IWrapperNBT connectionData = MasterLoader.coreInterface.createNewTag();
				priorConnection.save(connectionData);
				data.setData("priorConnection", connectionData);
			}
			if(nextConnection != null){
				IWrapperNBT connectionData = MasterLoader.coreInterface.createNewTag();
				nextConnection.save(connectionData);
				data.setData("nextConnection", connectionData);
			}
		}
		
		/**
		 *  Helper class for containing connecton data..
		 */
		private class RoadLaneConnection{
			public final Point3i tileLocation;
			public final int laneNumber;
			public final boolean connectedToStart;
			
			public RoadLaneConnection(Point3i tileLocation, int laneNumber, boolean connectedToStart){
				this.tileLocation = tileLocation;
				this.laneNumber = laneNumber;
				this.connectedToStart = connectedToStart;
			}
			
			public RoadLaneConnection(IWrapperNBT data){
				this.tileLocation = data.getPoint3i("tileLocation");
				this.laneNumber = data.getInteger("laneNumber");
				this.connectedToStart = data.getBoolean("connectedToStart");
			}
			
			public void save(IWrapperNBT data){
				data.setPoint3i("tileLocation",tileLocation);
				data.setInteger("laneNumber", laneNumber);
				data.setBoolean("connectedToStart", connectedToStart);
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
