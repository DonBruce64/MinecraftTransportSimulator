package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.baseclasses.RoadCurve;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
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
	public final Map<RoadComponent, ItemRoadComponent> components = new HashMap<RoadComponent, ItemRoadComponent>();
	public final Point3d[] curveConnectionPoints;
	public final Point3i[] backwardsBlockPositions;
	public final Point3i[] forwardsBlockPositions;
	public final Point3d[] forwardsCurveConnectionPoints;
	public final Double[] forwardsCurveRotations;
	public final RoadCurve[] curves;
	public final BoundingBox boundingBox;
	
	public static final int MAX_CURVE_CONNECTIONS = 3;
	public static final int MAX_SEGMENT_LENGTH = 32;
	
	public boolean holographic;
	
	public TileEntityRoad(IWrapperWorld world, Point3i position, IWrapperNBT data){
		super(world, position, data);
		//Load components back in.
		for(RoadComponent componentType : RoadComponent.values()){
			String packID = data.getString("packID" + componentType.ordinal());
			if(!packID.isEmpty()){
				String systemName = data.getString("systemName" + componentType.ordinal());
				ItemRoadComponent newComponent = PackParserSystem.getItem(packID, systemName);
				components.put(componentType, newComponent);
			}
		}
		
		//Create a Point3d for rotation operations.
		Point3d totalRotation = new Point3d(0, rotation, 0);
		
		//Now generate the curve connection points.  These come from our definition and our current rotation.
		curveConnectionPoints = new Point3d[definition.general.laneOffsets.length];
		for(int laneNumber=0; laneNumber<definition.general.laneOffsets.length; ++laneNumber){
			curveConnectionPoints[laneNumber] = new Point3d(definition.general.laneOffsets[laneNumber], 0, 0).rotateFine(totalRotation);
		}
		
		//Get saved curve connections that go to the generated points.
		//For blocks, we just store them until needed.  For curves, we use the points to generate them.
		//The reason we store the points and the block positions is because the blocks for the curves may not
		//get loaded in-order, but we need to know where the curve points are on construction. 
		this.backwardsBlockPositions = new Point3i[curveConnectionPoints.length];
		this.forwardsBlockPositions = new Point3i[curveConnectionPoints.length];
		this.forwardsCurveConnectionPoints = new Point3d[curveConnectionPoints.length];
		this.forwardsCurveRotations = new Double[curveConnectionPoints.length];
		this.curves = new RoadCurve[curveConnectionPoints.length];
		for(int laneNumber=0; laneNumber<curveConnectionPoints.length; ++laneNumber){
			Point3i loadedBackwardsPosition = data.getPoint3i("backwardsBlockPosition" + laneNumber);
			Point3i loadedForwardsPosition = data.getPoint3i("forwardsBlockPosition" + laneNumber);
			if(!loadedBackwardsPosition.isZero()){
				backwardsBlockPositions[laneNumber] = loadedBackwardsPosition;
			}
			if(!loadedForwardsPosition.isZero()){
				forwardsBlockPositions[laneNumber] = loadedForwardsPosition;
				forwardsCurveConnectionPoints[laneNumber] = data.getPoint3d("forwardsCurveConnectionPoint" + laneNumber);
				forwardsCurveRotations[laneNumber] = data.getDouble("forwardsCurveRotation" + laneNumber);
				curves[laneNumber] = new RoadCurve(forwardsCurveConnectionPoints[laneNumber].copy().subtract(curveConnectionPoints[laneNumber]), (float) rotation, forwardsCurveRotations[laneNumber].floatValue());
			}
		}
		
		//Set the bounding box.
		this.boundingBox = new BoundingBox(new Point3d(0, (definition.general.collisionHeight - 16)/16D/2D, 0), 0.5D, definition.general.collisionHeight/16D/2D, 0.5D);
		
		//Get the holographic state.
		this.holographic = data.getBoolean("holographic");
	}
	
	public void setCurve(int laneNumber, TileEntityRoad endTile, int endLaneNumber, boolean reversed){
		forwardsBlockPositions[laneNumber] = endTile.position;
		forwardsCurveConnectionPoints[laneNumber] = endTile.curveConnectionPoints[endLaneNumber].copy().add(endTile.position.x, endTile.position.y, endTile.position.z).add(-position.x, -position.y, -position.z);
		forwardsCurveRotations[laneNumber] = reversed ? endTile.rotation : endTile.rotation + 180;
		curves[laneNumber] = new RoadCurve(forwardsCurveConnectionPoints[laneNumber], (float) rotation, forwardsCurveRotations[laneNumber].floatValue());
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
		
		//Save curve connection data.
		for(int laneNumber=0; laneNumber<curveConnectionPoints.length; ++laneNumber){
			if(backwardsBlockPositions[laneNumber] != null){
				data.setPoint3i("backwardsBlockPosition" + laneNumber, backwardsBlockPositions[laneNumber]);
			}
			if(forwardsBlockPositions[laneNumber] != null){
				TileEntityRoad connectedRoad = world.getTileEntity(forwardsBlockPositions[laneNumber]);
				if(connectedRoad != null){
					data.setPoint3i("forwardsBlockPosition" + laneNumber, forwardsBlockPositions[laneNumber]);
					data.setPoint3d("forwardsCurveConnectionPoint" + laneNumber, forwardsCurveConnectionPoints[laneNumber]);
					data.setDouble("forwardsCurveRotation" + laneNumber, forwardsCurveRotations[laneNumber]);
				}
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
