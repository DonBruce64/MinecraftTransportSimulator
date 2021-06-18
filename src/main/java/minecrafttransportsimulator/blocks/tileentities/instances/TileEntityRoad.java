package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.RoadClickData;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONLaneSector;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONRoadCollisionArea;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadCollisionUpdate;
import minecrafttransportsimulator.rendering.instances.RenderRoad;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.item.ItemStack;

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
	public final List<Point3d> collisionBlockOffsets;
	public final List<Point3d> collidingBlockOffsets;
	
	public static final int MAX_COLLISION_DISTANCE = 32;
	private static RenderRoad renderer;
	
	public TileEntityRoad(WrapperWorld world, Point3d position, WrapperNBT data){
		super(world, position, data);
		//Set the bounding box.
		this.boundingBox = new BoundingBox(new Point3d(0, (definition.road.collisionHeight - 16)/16D/2D, 0), 0.5D, definition.road.collisionHeight/16D/2D, 0.5D);
		
		//Get the active state.
		this.isActive = data.getBoolean("isActive");
		
		//Load components back in.  Our core component will always be our definition.
		for(RoadComponent componentType : RoadComponent.values()){
			String packID = data.getString("packID" + componentType.ordinal());
			if(!packID.isEmpty()){
				String systemName = data.getString("systemName" + componentType.ordinal());
				ItemRoadComponent newComponent = PackParserSystem.getItem(packID, systemName);
				components.put(componentType, newComponent);
			}
		}
		components.put(RoadComponent.CORE, (ItemRoadComponent) getItem());
		
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
		
		//If we have points for collision due to use creating collision blocks, load them now.
		this.collisionBlockOffsets = data.getPoint3dsCompact("collisionBlockOffsets");
		this.collidingBlockOffsets = data.getPoint3dsCompact("collidingBlockOffsets");
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
	public void addDropsToList(List<ItemStack> drops){
		for(RoadComponent componentType : RoadComponent.values()){
			if(components.containsKey(componentType)){
				drops.add(components.get(componentType).getNewStack());
			}
		}
	}
	
	/**
	 *  Helper method to get information on what was clicked.
	 *  Takes the player's rotation into account, as well as the block they clicked.
	 */
	public RoadClickData getClickData(Point3d blockOffsetClicked, boolean curveStart){
		boolean clickedStart = blockOffsetClicked.isZero() || collisionBlockOffsets.indexOf(blockOffsetClicked) < collisionBlockOffsets.size()/2;
		JSONLaneSector closestSector = null;
		if(!definition.road.isDynamic){
			double closestSectorDistance = Double.MAX_VALUE;
			for(RoadLane lane : lanes){
				//Only check start points.  End points are for other sectors.
				double distanceToSectorStart = lane.curves.get(0).startPos.distanceTo(blockOffsetClicked);
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
		if(definition.road.isDynamic){
			for(int i=0; i<definition.road.laneOffsets.length; ++i){
				lanes.add(new RoadLane(this, 0, lanes.size(), data != null ? data.getData("lane" + lanes.size()) : null));
			}
		}else{
			for(int i=0; i<definition.road.sectors.size(); ++i){
				for(int j=0; j<definition.road.sectors.get(i).lanes.size(); ++j){
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
	protected Map<Point3d, Integer> generateCollisionPoints(){
		collisionBlockOffsets.clear();
		collidingBlockOffsets.clear();
		Map<Point3d, Integer> collisionHeightMap = new HashMap<Point3d, Integer>();
		if(definition.road.isDynamic){
			//Get all the points that make up our collision points for our dynamic curve.
			//If we find any colliding points, note them.
			Point3d testOffset = new Point3d();
			Point3d testRotation = new Point3d();
			float segmentDelta = (float) (definition.road.borderOffset/(Math.floor(definition.road.borderOffset) + 1));
			for(float f=0; f<dynamicCurve.pathLength; f+=0.1){
				for(float offset=0; offset < definition.road.borderOffset; offset += segmentDelta){
					dynamicCurve.setPointToRotationAt(testRotation, f);
					//We only want yaw for block placement.
					testRotation.x = 0;
					testRotation.z = 0;
					testOffset.set(offset, 0, 0).rotateCoarse(testRotation).add(0, definition.road.collisionHeight/16F, 0);
					dynamicCurve.offsetPointByPositionAt(testOffset, f);
					Point3d testPoint = new Point3d((int) testOffset.x, (int) Math.floor(testOffset.y), (int) testOffset.z);
					
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
				for(double x=collisionArea.firstCorner.x; x<=collisionArea.secondCorner.x; x += 0.5){
					for(double z=collisionArea.firstCorner.z; z<=collisionArea.secondCorner.z; z += 0.5){
						Point3d testPoint = new Point3d(x, 0, z).rotateFine(rotation);
						
						if(!testPoint.isZero() && !collisionBlockOffsets.contains(testPoint) && !collidingBlockOffsets.contains(testPoint)){
							//Offset the point to the global cordinate space, get the block, and offset back.
							testPoint.add(position);
							if(world.isAir(testPoint)){
								//Need a collision box here.
								testPoint.subtract(position);
								collisionBlockOffsets.add(testPoint);
								collisionHeightMap.put(testPoint, collisionArea.collisionHeight);
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
		Map<Point3d, Integer> collisionHeightMap = generateCollisionPoints();
		if(collidingBlockOffsets.isEmpty() || (player.isCreative() && player.isOP())){
			for(Point3d offset : collisionBlockOffsets){
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
