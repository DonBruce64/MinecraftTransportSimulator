package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.rendering.instances.RenderPole;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Pole tile entity.  Remembers what components we have attached and the state of the components.
 * This tile entity does not tick, as states can be determined without ticks or are controlled
 * from other tickable TEs.
*
* @author don_bruce
*/
public class TileEntityPole extends ATileEntityBase<JSONPoleComponent>{
	public final Map<Axis, ATileEntityPole_Component> components = new HashMap<Axis, ATileEntityPole_Component>();
	
	public TileEntityPole(WrapperWorld world, Point3i position, WrapperNBT data){
		super(world, position, data);
		//Load components back in.
		for(Axis axis : Axis.values()){
			String packID = data.getString("packID" + axis.ordinal());
			if(!packID.isEmpty()){
				String systemName = data.getString("systemName" + axis.ordinal());
				ATileEntityPole_Component newComponent = TileEntityPole.createComponent(PackParserSystem.getDefinition(packID, systemName));
				components.put(axis, newComponent);
				if(newComponent.getTextLines() != null){
					newComponent.setTextLines(data.getStrings("textLines", newComponent.getTextLines().size()));
				}
			}
		}
	}
	
	/**
	 * Helper method to update light state and re-do world lighting if required.
	 */
	public void updateLightState(){
		float calculatedLevel = 0;
		for(ATileEntityPole_Component component : components.values()){
			calculatedLevel = Math.max(calculatedLevel, component.lightLevel());
		}
		if(lightLevel != calculatedLevel){
			lightLevel = calculatedLevel;
			world.updateLightBrightness(position);
		}
	}
	
	@Override
	public List<AItemPack<JSONPoleComponent>> getDrops(){
		List<AItemPack<JSONPoleComponent>> drops = new ArrayList<AItemPack<JSONPoleComponent>>();
		for(Axis axis : Axis.values()){
			if(components.containsKey(axis)){
				drops.add(PackParserSystem.getItem(components.get(axis).definition));
			}
		}
		return drops;
	}
	
	@Override
	public RenderPole getRenderer(){
		return new RenderPole();
	}
	
	@Override
    public void save(WrapperNBT data){
		super.save(data);
		//Save all components.
		for(Entry<Axis, ATileEntityPole_Component> connectedObjectEntry : components.entrySet()){
			data.setString("packID" + connectedObjectEntry.getKey().ordinal(), connectedObjectEntry.getValue().definition.packID);
			data.setString("systemName" + connectedObjectEntry.getKey().ordinal(), connectedObjectEntry.getValue().definition.systemName);
			if(connectedObjectEntry.getValue().getTextLines() != null){
				data.setStrings("textLines", connectedObjectEntry.getValue().getTextLines());
			}
		}
    }
	
	/**
	 *  Helper method to create a component for this TE.  Does not add the component.
	 */
	public static ATileEntityPole_Component createComponent(JSONPoleComponent definition){
		switch(definition.general.type){
			case("core") : return new TileEntityPole_Core(definition);	
			case("traffic_signal") : return new TileEntityPole_TrafficSignal(definition);
			case("street_light") : return new TileEntityPole_StreetLight(definition);
			case("sign") : return new TileEntityPole_Sign(definition);
			default : throw new IllegalArgumentException("ERROR: Wanted type: " + (definition.general.type != null ? definition.general.type : null) + " for pole:" + definition.packID + ":" + definition.systemName +", but such a type is not a valid pole component.  Contact the pack author." );
		}
	}
}
