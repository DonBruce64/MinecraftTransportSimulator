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
import minecrafttransportsimulator.blocks.tileentities.components.TileEntityRoad_Component;
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
 * curves connected to and from each point.  This is why the arrays are two-dimensional.
 *
 * @author don_bruce
 */
public class TileEntityRoad extends ATileEntityBase<JSONRoadComponent>{
	public final Map<RoadComponent, TileEntityRoad_Component> components = new HashMap<RoadComponent, TileEntityRoad_Component>();
	public final Point3d centerOffset;
	public final Point3d[] curveConnectionPoints;
	public final Point3i[][] backwardsBlockPositions;
	public final Point3i[][] forwardsBlockPositions;
	public final RoadCurve[][] curves;
	public final BoundingBox boundingBox;
	
	private static final int MAX_CURVE_CONNECTIONS = 3;
	
	public TileEntityRoad(IWrapperWorld world, Point3i position, IWrapperNBT data){
		super(world, position, data);
		//Load components back in.
		for(RoadComponent componentType : RoadComponent.values()){
			String packID = data.getString("packID" + componentType.ordinal());
			if(!packID.isEmpty()){
				String systemName = data.getString("systemName" + componentType.ordinal());
				TileEntityRoad_Component newComponent = createComponent(PackParserSystem.getItem(packID, systemName));
				components.put(componentType, newComponent);
			}
		}
		
		//Get the centerOffset.  For blocks placed directly, this will be 0,0,0.
		//For blocks placed as connectors to existing curves, or those as midpoints, it will be non-zero.
		//Offset may be positive or negative for any point, but is assured to be between -0.5 and 0.5 for each axis.
		centerOffset = data.getPoint3d("centerOffset");
		
		//Create a Point3d for rotation operations.
		Point3d totalRotation = new Point3d(0, rotation, 0);
		
		//Now generate the curve connection points.  These come from our definition and our current rotation.
		curveConnectionPoints = new Point3d[definition.general.numberLanes];
		for(int i=0; i<definition.general.numberLanes; ++i){
			curveConnectionPoints[i] = new Point3d(position).add(definition.general.firstLaneOffset + i*definition.general.laneWidth, 0, 0).rotateFine(totalRotation);
		}
		
		//Get saved curve connections that go to the generated points.
		this.backwardsBlockPositions = new Point3i[curveConnectionPoints.length][MAX_CURVE_CONNECTIONS];
		this.forwardsBlockPositions = new Point3i[curveConnectionPoints.length][MAX_CURVE_CONNECTIONS];
		this.curves = new RoadCurve[curveConnectionPoints.length][MAX_CURVE_CONNECTIONS];
		for(int i=0; i<curveConnectionPoints.length; ++i){
			for(int j=0; j<MAX_CURVE_CONNECTIONS; ++j){
				Point3i loadedBackwardsPosition = data.getPoint3i("backwardsBlockPosition" + String.valueOf(i) + String.valueOf(j));
				Point3i loadedForwardsPosition = data.getPoint3i("forwardsBlockPosition" + String.valueOf(i) + String.valueOf(j));
				if(!loadedBackwardsPosition.isZero()){
					backwardsBlockPositions[i][j] = loadedBackwardsPosition;
				}
				if(!loadedForwardsPosition.isZero()){
					forwardsBlockPositions[i][j] = loadedForwardsPosition;
				}
			}
		}
		
		//Set the bounding box.
		this.boundingBox = new BoundingBox(new Point3d(0, 0, 0), 0.5D, definition.general.collisionHeight/2D, 0.5D);
	}
	
	@Override
	public List<AItemPack<JSONRoadComponent>> getDrops(){
		List<AItemPack<JSONRoadComponent>> drops = new ArrayList<AItemPack<JSONRoadComponent>>();
		for(RoadComponent componentType : RoadComponent.values()){
			if(components.containsKey(componentType)){
				drops.add(components.get(componentType).item);
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
		for(Entry<RoadComponent, TileEntityRoad_Component> connectedObjectEntry : components.entrySet()){
			data.setString("packID" + connectedObjectEntry.getKey().ordinal(), connectedObjectEntry.getValue().definition.packID);
			data.setString("systemName" + connectedObjectEntry.getKey().ordinal(), connectedObjectEntry.getValue().definition.systemName);
		}
		
		//Save curve connection data.
		for(int i=0; i<curveConnectionPoints.length; ++i){
			for(int j=0; j<MAX_CURVE_CONNECTIONS; ++j){
				data.setPoint3i("backwardsBlockPosition" + String.valueOf(i) + String.valueOf(j), backwardsBlockPositions[i][j]);
				data.setPoint3i("forwardsBlockPosition" + String.valueOf(i) + String.valueOf(j), forwardsBlockPositions[i][j]);
			}
		}
    }
	
	/**
	 *  Helper method to create a component for this TE.  Does not add the component.
	 */
	public static TileEntityRoad_Component createComponent(ItemRoadComponent item){
		for(RoadComponent component : RoadComponent.values()){
			if(component.name().toLowerCase().endsWith(item.definition.general.type)){
				return new TileEntityRoad_Component(item);
			}
		}
		throw new IllegalArgumentException("ERROR: Wanted type: " + (item.definition.general.type != null ? item.definition.general.type : null) + " for road component:" + item.definition.packID + ":" + item.definition.systemName +", but such a type is not a valid road component.  Contact the pack author." );
	}
	
	/**
	 *  Enums for part-specific stuff.
	 */
	public static enum RoadComponent{
		CORE,
		LEFT_MARKING,
		RIGHT_MARKING,
		CENER_MARKING,
		LEFT_BORDER,
		RIGHT_BORDER,
		UNDERLAYMENT,
		SUPPORT;
	}
}
