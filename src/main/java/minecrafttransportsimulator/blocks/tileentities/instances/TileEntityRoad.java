package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
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
	//public final Point3d[] curveConnectionPoints;
	//public final Point3i[][] backwardsBlockPosition;
	//public final Point3i[][] forwardsBlockPosition;
	//public final RoadCurve[][] curves;
	public final BoundingBox boundingBox;
	
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
		
		//FIXME Generate the curve connection points.
		
		//FIXME Get saved curve connections that go to the generated points.
		
		
		//Set the bounding box.
		this.boundingBox = new BoundingBox(new Point3d(0, 0, 0), definition.general.width/2D, definition.general.collisionHeight/2D, definition.general.width/2D);
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
