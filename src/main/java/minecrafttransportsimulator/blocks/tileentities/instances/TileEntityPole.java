package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.rendering.blocks.RenderPole;
import minecrafttransportsimulator.wrappers.WrapperNBT;

/**Pole tile entity.  Remembers what components we have attached and the state of the components.
 * This tile entity does not tick, as states can be determined without ticks or are controlled
 * from other tickable TEs.
*
* @author don_bruce
*/
public class TileEntityPole extends ATileEntityBase<JSONPoleComponent>{
	public final Map<Axis, ATileEntityPole_Component> components = new HashMap<Axis, ATileEntityPole_Component>();
	public final Map<Axis, Boolean> connections = new HashMap<Axis, Boolean>();
	public final Map<Axis, Boolean> solidConnections = new HashMap<Axis, Boolean>();
	
	@Override
	@SuppressWarnings("unchecked")
    public void load(WrapperNBT data){
		super.load(data);
		//Load components back in.
		for(Axis axis : Axis.values()){
			String packID = data.getString("packID" + axis.ordinal());
			if(packID != null){
				String systemName = data.getString("systemName" + axis.ordinal());
				AItemPack<JSONPoleComponent> component = (AItemPack<JSONPoleComponent>) MTSRegistry.packItemMap.get(packID).get(systemName);
				components.put(axis, createComponent(component.definition));
				if(component.definition.general.textLines != null){
					//Assume we are a sign and add text.
					((TileEntityPole_Sign) components.get(axis)).textLines.addAll(data.getStrings("textLines", component.definition.general.textLines.length));
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
			if(connectedObjectEntry.getValue() instanceof TileEntityPole_Sign){
				data.setStrings("textLines", ((TileEntityPole_Sign) connectedObjectEntry.getValue()).textLines);
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
			case("crossing_signal") : return new TileEntityPole_CrossingSignal(definition);
			case("street_light") : return new TileEntityPole_StreetLight(definition);
			//Sign is assumed to be default per legacy systems.
			default : return new TileEntityPole_Sign(definition);
		}
	}
}
