package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.rendering.instances.RenderPole;
import minecrafttransportsimulator.wrappers.WrapperNBT;

/**Pole tile entity.  Remembers what components we have attached and the state of the components.
 * This tile entity does not tick, as states can be determined without ticks or are controlled
 * from other tickable TEs.
*
* @author don_bruce
*/
public class TileEntityPole extends ATileEntityBase<JSONPoleComponent>{
	public final Map<Axis, ATileEntityPole_Component> components = new HashMap<Axis, ATileEntityPole_Component>();
	
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
	public List<AItemPack<? extends AJSONItem<? extends AJSONItem<?>.General>>> getDrops(){
		List<AItemPack<? extends AJSONItem<? extends AJSONItem<?>.General>>> drops = new ArrayList<AItemPack<? extends AJSONItem<? extends AJSONItem<?>.General>>>();
		for(Axis axis : Axis.values()){
			if(components.containsKey(axis)){
				drops.add(MTSRegistry.packItemMap.get(components.get(axis).definition.packID).get(components.get(axis).definition.systemName));
			}
		}
		return drops;
	}
	
	@Override
	@SuppressWarnings("unchecked")
    public void load(WrapperNBT data){
		super.load(data);
		//Load components back in.
		for(Axis axis : Axis.values()){
			String packID = data.getString("packID" + axis.ordinal());
			if(!packID.isEmpty()){
				String systemName = data.getString("systemName" + axis.ordinal());
				AItemPack<JSONPoleComponent> componentItem = (AItemPack<JSONPoleComponent>) MTSRegistry.packItemMap.get(packID).get(systemName);
				ATileEntityPole_Component newComponent = TileEntityPole.createComponent(componentItem.definition);
				components.put(axis, newComponent);
				if(newComponent.getTextLines() != null){
					newComponent.setTextLines(data.getStrings("textLines", newComponent.getTextLines().size()));
				}
			}
		}
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
	
	@Override
	public RenderPole getRenderer(){
		return new RenderPole();
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
