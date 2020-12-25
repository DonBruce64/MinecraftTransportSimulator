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
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockRoad;
import minecrafttransportsimulator.blocks.instances.BlockRoadCollision;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
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
	public final Point3d startingOffset;
	public BezierCurve curve;
	public final List<RoadLane> lanes;

	//Dynamic variables based on states.
	public final Map<RoadComponent, ItemRoadComponent> components = new HashMap<RoadComponent, ItemRoadComponent>();
	public final List<Point3i> collidingBlockOffsets = new ArrayList<Point3i>();
	public boolean isActive;
	
	//Static constants.
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
		
		//Load curve and lane data.  We may not have this yet if we're in the process of creating a new road.
		this.lanes = new ArrayList<RoadLane>();
		this.startingOffset = data.getPoint3d("startingOffset");
		Point3d endingOffset = data.getPoint3d("endingOffset");
		if(!endingOffset.isZero()){
			this.curve = new BezierCurve(endingOffset, (float) data.getDouble("startingRotation"), (float) data.getDouble("endingRotation"));
			for(int laneNumber=0; laneNumber < definition.general.laneOffsets.length; ++laneNumber){
				lanes.add(new RoadLane(this, data.getData("lane" + laneNumber)));
			}
		}else{
			float[] definitionOffsets = definition.general.laneOffsets;
			for(int laneNumber=0; laneNumber < definitionOffsets.length; ++laneNumber){
				Point3d laneOffset = new Point3d(definitionOffsets[laneNumber], 0, 0).rotateFine(new Point3d(0, rotation, 0));
				lanes.add(new RoadLane(this, laneOffset));
			}
		}
		
		//If we have points for collision due to use creating collision blocks, load them now.
		this.collisionBlockOffsets = data.getPoints("collisionBlockOffsets");
		
		//Get the active state.
		this.isActive = data.getBoolean("isActive");
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
	public RoadClickData getClickData(Point3i blockClicked, IWrapperPlayer player){
		//First check if we clicked the start or end of the curve.
		Point3i clickedOffset = blockClicked.copy().subtract(position);
		boolean clickedStart = clickedOffset.isZero() || collisionBlockOffsets.indexOf(clickedOffset) < collisionBlockOffsets.size()/2;
		
		//Next check how many lanes the road the player has is holding.  This affects which lane we say they clicked.
		int lanesOnHeldRoad = 0;
		AItemBase heldItem = player.getHeldItem();
		if(heldItem instanceof ItemRoadComponent){
			if(((ItemRoadComponent) heldItem).definition.general.laneOffsets != null){
				lanesOnHeldRoad = ((ItemRoadComponent) heldItem).definition.general.laneOffsets.length;
			}
		}
		
		//Next get the angle between the player and the side of the curve they clicked.
		double angleDelta;
		if(clickedStart){
			angleDelta = player.getYaw() - curve.startAngle;
		}else{
			angleDelta = player.getYaw() - curve.endAngle + 180;
		}
		while(angleDelta < -180)angleDelta += 360;
		while(angleDelta > 180)angleDelta -= 360;
		
		//Based on the angle, and the lane on our held item, and what side we clicked, return click data.
		//Try to keep the lane in the center by applying an offset if we're clicking with a road with only a few lanes.
		//FIXME check math here later.
		boolean clickedForward = clickedStart ? Math.abs(angleDelta) < 90 : Math.abs(angleDelta) > 90;
		int laneClicked;
		if(clickedForward){
			laneClicked = (int) angleDelta/25;
			if(lanesOnHeldRoad < lanes.size()){
				laneClicked += (lanes.size() - lanesOnHeldRoad)/2;
			}
		}else{
			laneClicked = lanes.size() - (int) angleDelta/25;
			if(lanesOnHeldRoad < lanes.size()){
				laneClicked -= (lanes.size() - lanesOnHeldRoad)/2;
			}
		}
		
		//Finally, return the data in object form.
		return new RoadClickData(this, laneClicked, clickedStart, clickedForward);
	}
	
	/**
	 *  Helper method to spawn collision boxes for this road.  Returns true and makes
	 *  this road non-holographic if the boxes could be spawned.  False if there are
	 *  blocking blocks.  OP and creative-mode players override blocking block checks.
	 *  Road width is considered to extend to the left and right border, minus 1/2 a block.
	 */
	public boolean spawnCollisionBlocks(IWrapperPlayer player){
		//Get all the points that make up our collision points.
		//If we find any colliding points, note them.
		Point3d testOffset = new Point3d(0, 0, 0);
		Point3d testRotation = new Point3d(0, 0, 0);
		Map<Point3i, Integer> collisionHeightMap = new HashMap<Point3i, Integer>();
		float segmentDelta = (float) (definition.general.borderOffset/(Math.floor(definition.general.borderOffset) + 1));
		for(float f=0; f<curve.pathLength; f+=0.1){
			for(float offset=0; offset < definition.general.borderOffset; offset += segmentDelta){
				curve.setPointToRotationAt(testRotation, f);
				//We only want yaw for block placement.
				testRotation.x = 0;
				testRotation.z = 0;
				testOffset.set(offset, 0, 0).rotateCoarse(testRotation).add(0, definition.general.collisionHeight/16F, 0);
				curve.offsetPointbyPositionAt(testOffset, f);
				Point3i testPoint = new Point3i((int) testOffset.x, (int) Math.floor(testOffset.y), (int) testOffset.z);
				
				//If we don't have a block in this position, check if we need one.
				if(!collisionBlockOffsets.contains(testPoint) && !collidingBlockOffsets.contains(testPoint)){
					//Offset the point to the global cordinate space, get the block, and offset back.
					testPoint.add(position);
					ABlockBase testBlock = world.getBlock(testPoint);
					testPoint.subtract(position);
					if(testBlock == null){
						//Need a collision box here.
						int collisionBoxIndex = (int) ((testOffset.y - testPoint.y)*16);
						collisionBlockOffsets.add(testPoint);
						
						collisionHeightMap.put(testPoint, collisionBoxIndex);
					}else if(!(testBlock instanceof BlockRoadCollision || testBlock instanceof BlockRoad)){
						//Some block is blocking us that's not part of a road.  Flag it.
						collidingBlockOffsets.add(testPoint);
					}
				}
			}
		}
		
		if(collidingBlockOffsets.isEmpty() || (player.isCreative() && player.isOP())){
			for(Point3i offset : collisionBlockOffsets){
				Point3i testPoint = offset.copy().add(position);
				world.setBlock(BlockRoadCollision.blocks.get(collisionHeightMap.get(offset)), testPoint, null, Axis.UP);
			}
			collidingBlockOffsets.clear();
			isActive = true;
			return true;
		}else{
			collisionBlockOffsets.clear();
			return false;
		}
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
		
		//Save curve data.
		data.setPoint3d("startingOffset", startingOffset);
		data.setPoint3d("endingOffset", curve.endPos);
		data.setDouble("startingRotation", curve.startAngle);
		data.setDouble("endingRotation", curve.endAngle);
		
		//Save lane data.
		for(int laneNumber=0; laneNumber < lanes.size(); ++laneNumber){
			RoadLane lane = lanes.get(laneNumber);
			IWrapperNBT laneData = MasterLoader.coreInterface.createNewTag();
			lane.save(laneData);
			data.setData("lane" + laneNumber, laneData);
		}
		
		//Save cure collision point data.
		data.setPoints("collisionBlockOffsets", collisionBlockOffsets);
		
		//Save isActive state.
		data.setBoolean("isActive", isActive);
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
