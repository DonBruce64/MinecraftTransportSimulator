package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.rendering.components.ITextProvider;
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
				String subName = data.getString("subName" + axis.ordinal());
				ATileEntityPole_Component newComponent = PoleComponentType.createComponent(this, PackParserSystem.getItem(packID, systemName, subName));
				components.put(axis, newComponent);
				
				if(newComponent instanceof ITextProvider && newComponent.definition.rendering != null && newComponent.definition.rendering.textObjects != null){
					ITextProvider provider = (ITextProvider) newComponent;
					for(int i=0; i<newComponent.definition.rendering.textObjects.size(); ++i){
						provider.getText().put(newComponent.definition.rendering.textObjects.get(i), data.getString("textLine" + axis.ordinal() + i));
					}
				}
			}
		}
		
		//If we don't have our core component on the NONE axis, add it now based on our definition.
		//This is done for ease of rendering and lookup routines.
		if(!components.containsKey(Axis.NONE)){
			components.put(Axis.NONE, PoleComponentType.createComponent(this, ((ItemPoleComponent) this.item)));
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
				drops.add(components.get(axis).item);
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
			Axis axis = connectedObjectEntry.getKey();
			ATileEntityPole_Component component = connectedObjectEntry.getValue();
			data.setString("packID" + axis.ordinal(), component.definition.packID);
			data.setString("systemName" + axis.ordinal(), component.definition.systemName);
			data.setString("subName" + axis.ordinal(), component.item.subName);
			if(component instanceof ITextProvider){
				int lineNumber = 0;
				for(String textLine : ((ITextProvider) component).getText().values()){
					data.setString("textLine" + axis.ordinal() + lineNumber++, textLine);
				}
			}
		}
    }
	
	public static enum PoleComponentType{
		@JSONDescription("The base of any pole system is the core type. This is the central structure that connects to other pole bits and allows placement of components on it.  You cannot place other components without placing one of these first.")
		CORE,
		@JSONDescription("Perhaps the most standard of lights, traffic signals consist of a main model (named anything you like), plus the lights (see the lights section).  You may omit any or all lights should you wish to change your sinal's behavior.  This may include making fewer or more bulbs than the standard 3 light.  Say a 2-light unit for a crossing signal.")
		TRAFFIC_SIGNAL,
		@JSONDescription("These are the simplest type of lights.  Designed for street accents, the lights normally don't change state.  However, signal controllers can turn their light off via a redstone input.")
		STREET_LIGHT,
		@JSONDescription("Signs are the third pole component you can create, and perhaps one of the most overlooked pack-based things in MTS.  Signs may have lights on them as well, and behave the same as street lights; the only exception being that their lights cannot be controlled by signal controllers.  If a sign has textObjects in its rendering section, then it will allow for editing that text via GUI.  This allows for dynamic route and speed limit signs, among others.")
		SIGN;
		
		/**
		 *  Helper method to create a component for the passed-in pole.  Does not add the component
		 *  to the pole, only creates it.
		 */
		public static ATileEntityPole_Component createComponent(TileEntityPole pole, ItemPoleComponent item){
			switch(item.definition.general.type){
				case CORE : return new TileEntityPole_Core(pole, item);	
				case TRAFFIC_SIGNAL : return new TileEntityPole_TrafficSignal(pole, item);
				case STREET_LIGHT : return new TileEntityPole_StreetLight(pole, item);
				case SIGN : return new TileEntityPole_Sign(pole, item);
			}
			//We'll never get here.
			return null;
		}
	}
}
