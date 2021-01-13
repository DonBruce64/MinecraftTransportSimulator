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
				ATileEntityPole_Component newComponent = createComponent(PackParserSystem.getItem(packID, systemName, subName));
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
			components.put(Axis.NONE, createComponent(((ItemPoleComponent) this.item)));
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
	
	/**
	 *  Helper method to create a component for this TE.  Does not add the component.
	 */
	public ATileEntityPole_Component createComponent(ItemPoleComponent itemAdded){
		switch(itemAdded.definition.general.type){
			case("core") : return new TileEntityPole_Core(this, itemAdded);	
			case("traffic_signal") : return new TileEntityPole_TrafficSignal(this, itemAdded);
			case("street_light") : return new TileEntityPole_StreetLight(this, itemAdded);
			case("sign") : return new TileEntityPole_Sign(this, itemAdded);
			default : throw new IllegalArgumentException("Wanted type: " + (itemAdded.definition.general.type != null ? itemAdded.definition.general.type : null) + " for pole:" + itemAdded.definition.packID + ":" + itemAdded.definition.systemName +", but such a type is not a valid pole component.  Contact the pack author." );
		}
	}
}
