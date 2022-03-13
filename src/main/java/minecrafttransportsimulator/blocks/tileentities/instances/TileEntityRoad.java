package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.RoadClickData;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONLaneSector;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONRoadCollisionArea;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadCollisionUpdate;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.rendering.components.RenderableObject;
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
	public BezierCurve dynamicCurve;
	public final List<RoadLane> lanes;

	//Dynamic variables based on states.
	private boolean isActive;
	public final Map<RoadComponent, ItemRoadComponent> components = new HashMap<RoadComponent, ItemRoadComponent>();
	public final Map<RoadComponent, RenderableObject> componentRenderables = new HashMap<RoadComponent, RenderableObject>();
	public final List<RenderableObject> devRenderables = new ArrayList<RenderableObject>();
	public final List<BoundingBox> blockingBoundingBoxes = new ArrayList<BoundingBox>();
	public final List<Point3D> collisionBlockOffsets;
	public final List<Point3D> collidingBlockOffsets;
	
	public static final int MAX_COLLISION_DISTANCE = 32;
	private static RenderRoad renderer;
	
	public TileEntityRoad(WrapperWorld world, Point3D position, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, position, placingPlayer, data);
		
		//Set the bounding box.
		this.boundingBox.heightRadius = definition.road.collisionHeight/16D/2D;
		this.boundingBox.globalCenter.y += boundingBox.heightRadius;
		
		//Get the active state.
		this.isActive = data.getBoolean("isActive");
		
		//Load components back in.  Our core component will always be our definition.
		for(RoadComponent componentType : RoadComponent.values()){
			String packID = data.getString("packID" + componentType.name());
			if(!packID.isEmpty()){
				String systemName = data.getString("systemName" + componentType.name());
				ItemRoadComponent newComponent = PackParserSystem.getItem(packID, systemName);
				components.put(componentType, newComponent);
			}
		}
		components.put(definition.road.type, (ItemRoadComponent) getItem());
		
		//Load curve and lane data.  We may not have this yet if we're in the process of creating a new road.
		this.lanes = new ArrayList<RoadLane>();
		Point3D startingOffset = data.getPoint3d("startingOffset");
		Point3D endingOffset = data.getPoint3d("endingOffset");
		if(!endingOffset.isZero()){
			this.dynamicCurve = new BezierCurve(startingOffset, endingOffset, new RotationMatrix().setToAngles(data.getPoint3d("startingAngles")), new RotationMatrix().setToAngles(data.getPoint3d("endingAngles")));
		}
		
		//Don't generate lanes for inactive roads.
		if(isActive()){
			generateLanes(data);
		}
		
		//If we have points for collision due to use creating collision blocks, load them now.
		this.collisionBlockOffsets = data.getPoint3dsCompact("collisionBlockOffsets");
		this.collidingBlockOffsets = data.getPoint3dsCompact("collidingBlockOffsets");
	}
	
	@Override
	public double getPlacementRotation(WrapperPlayer player){
		if(!definition.road.type.equals(RoadComponent.CORE_DYNAMIC)){
			int clampAngle = getRotationIncrement();
			//Normally blocks are placed facing us.  For roads though, we want us to have the angles the player is facing.
			return Math.round((player.getYaw())/clampAngle)*clampAngle%360;
		}else{
			return 0;
		}
	}
	
	/**
	 *  Returns true if this road is active.  Active roads may be considered
	 *  to have all their collision blocks and no blocking blocks.
	 */
	public boolean isActive(){
		return isActive;
	}
	
	/**
	 *  Sets this road as active or inactive.  This happens after collision creation for
	 *  setting it as active, or prior to destruction for inactive.  This method should
	 *  handle any logic that needs to happen once the block is active and valid.
	 */
	public void setActive(boolean active){
		isActive = active;
		if(active){
	    	generateLanes(null);
	    }
	}
	
	@Override
	public void addDropsToList(List<WrapperItemStack> drops){
		for(RoadComponent componentType : RoadComponent.values()){
			if(components.containsKey(componentType)){
				drops.add(components.get(componentType).getNewStack(null));
			}
		}
	}
	
	@Override
	public void remove(){
		super.remove();
		for(RenderableObject object : componentRenderables.values()){
			object.destroy();
		}
	}
	
	@Override
    public void destroy(BoundingBox box){
    	super.destroy(box);
		if(isActive){
			//Set the TE to inactive and remove all road connections.
			setActive(false);
			for(RoadLane lane : lanes){
				lane.removeConnections();
			}
			
			//Now remove all collision blocks.
			for(Point3D blockOffset : collisionBlockOffsets){
				Point3D blockLocation = position.copy().add(blockOffset);
				//Check to make sure we don't destroy non-road blocks.
				//This is required in case our TE is corrupt or someone messes with it.
				if(world.getBlock(blockLocation) instanceof BlockCollision){
					world.destroyBlock(blockLocation, true);
				}
			}
		}
    }
	
	@Override
	public boolean interact(WrapperPlayer player){
		//Check if we aren't active.  If not, try to spawn collision again.
    	if(!isActive){
    		spawnCollisionBlocks(player);
    		return true;
    	}else{
    		return false;
    	}
	}
	
	/**
	 *  Helper method to get information on what was clicked.
	 *  Takes the player's rotation into account, as well as the block they clicked.
	 */
	public RoadClickData getClickData(Point3D blockClicked, boolean curveStart){
		Point3D blockOffsetClicked = blockClicked.copy().add(0.5, 0, 0.5).subtract(position);
		boolean clickedStart = blockOffsetClicked.isZero() || collisionBlockOffsets.indexOf(blockOffsetClicked) < collisionBlockOffsets.size()/2;
		JSONLaneSector closestSector = null;
		if(!definition.road.type.equals(RoadComponent.CORE_DYNAMIC)){
			double closestSectorDistance = Double.MAX_VALUE;
			for(RoadLane lane : lanes){
				//Only check start points.  End points are for other sectors.
				double distanceToSectorStart = lane.curves.get(0).startPos.distanceTo(blockClicked);
				if(distanceToSectorStart < closestSectorDistance){
					closestSectorDistance = distanceToSectorStart;
					closestSector = definition.road.sectors.get(lane.sectorNumber);
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
		int totalLanes = 0;
		if(definition.road.type.equals(RoadComponent.CORE_DYNAMIC)){
			for(int i=0; i<definition.road.laneOffsets.length; ++i){
				lanes.add(new RoadLane(this, 0, i, 0, data != null ? data.getData("lane" + totalLanes) : null));
				++totalLanes;
			}
		}else{
			for(int i=0; i<definition.road.sectors.size(); ++i){
				for(int j=0; j<definition.road.sectors.get(i).lanes.size(); ++j){
					lanes.add(new RoadLane(this, i, totalLanes, j, data != null ? data.getData("lane" + totalLanes) : null));
					++totalLanes;
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
	protected Map<Point3D, Integer> generateCollisionPoints(){
		collisionBlockOffsets.clear();
		collidingBlockOffsets.clear();
		Map<Point3D, Integer> collisionHeightMap = new HashMap<Point3D, Integer>();
		if(definition.road.type.equals(RoadComponent.CORE_DYNAMIC)){
			//Get all the points that make up our collision points for our dynamic curve.
			//If we find any colliding points, note them.
			Point3D testOffset = new Point3D();
			float segmentDelta = (float) (definition.road.roadWidth/(Math.floor(definition.road.roadWidth) + 1));
			for(float f=0; f<dynamicCurve.pathLength; f+=0.1){
				for(float offset=0; offset <= definition.road.roadWidth; offset += segmentDelta){
					testOffset.set(offset, 0, 0).rotate(dynamicCurve.getRotationAt(f));
					dynamicCurve.offsetPointByPositionAt(testOffset, f);
					testOffset.subtract(position);
					Point3D testPoint = new Point3D((int) testOffset.x, (int) Math.floor(testOffset.y), (int) testOffset.z);
					
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
		}else{
			//Do static block additions for static component.
			for(JSONRoadCollisionArea collisionArea : definition.road.collisionAreas){
				for(double x=collisionArea.firstCorner.x+0.01; x<collisionArea.secondCorner.x+0.5; x += 0.5){
					for(double z=collisionArea.firstCorner.z+0.01; z<collisionArea.secondCorner.z+0.5; z += 0.5){
						Point3D testPoint = new Point3D(x, collisionArea.firstCorner.y, z).rotate(orientation);
						testPoint.x = (int) testPoint.x;
						testPoint.z = (int) testPoint.z;
						
						if(!testPoint.isZero() && !collisionBlockOffsets.contains(testPoint) && !collidingBlockOffsets.contains(testPoint)){
							//Offset the point to the global cordinate space, get the block, and offset back.
							testPoint.add(position);
							if(world.isAir(testPoint)){
								//Need a collision box here.
								testPoint.subtract(position);
								collisionBlockOffsets.add(testPoint);
								collisionHeightMap.put(testPoint, collisionArea.collisionHeight == 16 ? 15 : collisionArea.collisionHeight);
							}else if(!(world.getBlock(testPoint) instanceof BlockCollision)){
								//Some block is blocking us that's not part of a road.  Flag it.
								testPoint.subtract(position);
								collidingBlockOffsets.add(testPoint);
							}
						}
					}
				}
			}
		}
		return collisionHeightMap;
	}
	
	/**
	 *  Method to spawn collision boxes for this road structure.
	 *  Returns true and makes this TE active if all the boxes could be spawned.
	 *  False if there are blocking blocks.  OP and creative-mode players override blocking block checks.
	 */
	public boolean spawnCollisionBlocks(WrapperPlayer player){
		Map<Point3D, Integer> collisionHeightMap = generateCollisionPoints();
		if(collidingBlockOffsets.isEmpty() || (player.isCreative() && player.isOP())){
			for(Point3D offset : collisionBlockOffsets){
				world.setBlock(BlockCollision.blockInstances.get(collisionHeightMap.get(offset)), offset.copy().add(position), null, Axis.UP);
			}
			collidingBlockOffsets.clear();
			setActive(true);
			InterfacePacket.sendToAllClients(new PacketTileEntityRoadCollisionUpdate(this));
			return true;
		}else{
			collisionBlockOffsets.clear();
			player.sendPacket(new PacketPlayerChatMessage(player, "interact.roadcomponent.blockingblocks"));
			InterfacePacket.sendToAllClients(new PacketTileEntityRoadCollisionUpdate(this));
			return false;
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public RenderRoad getRenderer(){
		if(renderer == null){
			renderer = new RenderRoad();
		}
		return renderer;
	}
	
	@Override
    public WrapperNBT save(WrapperNBT data){
		super.save(data);
		//Save isActive state.
		data.setBoolean("isActive", isActive);
				
		//Save all components.
		for(Entry<RoadComponent, ItemRoadComponent> connectedObjectEntry : components.entrySet()){
			data.setString("packID" + connectedObjectEntry.getKey().name(), connectedObjectEntry.getValue().definition.packID);
			data.setString("systemName" + connectedObjectEntry.getKey().name(), connectedObjectEntry.getValue().definition.systemName);
		}
		
		//Save curve data.
		if(dynamicCurve != null){
			data.setPoint3d("startingOffset", dynamicCurve.startPos);
			data.setPoint3d("endingOffset", dynamicCurve.endPos);
			data.setPoint3d("startingAngles", dynamicCurve.startRotation.angles);
			data.setPoint3d("endingAngles", dynamicCurve.endRotation.angles);
		}
		
		//Save lane data.
		for(int laneNumber=0; laneNumber < lanes.size(); ++laneNumber){
			data.setData("lane" + laneNumber, lanes.get(laneNumber).getData());
		}
		
		//Save cure collision point data.
		data.setPoint3dsCompact("collisionBlockOffsets", collisionBlockOffsets);
		data.setPoint3dsCompact("collidingBlockOffsets", collidingBlockOffsets);
		return data;
    }
	
	/**
	 *  Enums for part-specific stuff.
	 */
	public static enum RoadComponent{
		@JSONDescription("The core component.  This must be placed down before any other road components.  This is a static component with defined lanes and collision.")
		CORE_STATIC,
		@JSONDescription("The core component.  This must be placed down before any other road components.  This is a dynamic component with flexible collision and lane paths, but defined lane counts and offsets.")
		CORE_DYNAMIC;
	}
}
