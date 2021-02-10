package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.instances.AnimationsPole;
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
	
	private static final AnimationsPole animator = new AnimationsPole();
	private static RenderPole renderer;
	
	public TileEntityPole(WrapperWorld world, Point3d position, WrapperNBT data){
		super(world, position, data);
		//Load components back in.
		for(Axis axis : Axis.values()){
			if(!axis.equals(Axis.NONE)){
				WrapperNBT componentData = data.getData(axis.name());
				if(componentData != null){
					ATileEntityPole_Component newComponent = PoleComponentType.createComponent(this, PackParserSystem.getItem(componentData.getString("packID"), componentData.getString("systemName"), componentData.getString("subName")), componentData);
					components.put(axis, newComponent);
				}
			}
		}
		
		//Add our core component to the NONE axis.
		//This is done for ease of rendering and lookup routines.
		if(!components.containsKey(Axis.NONE)){
			components.put(Axis.NONE, PoleComponentType.createComponent(this, PackParserSystem.getItem(definition.packID, definition.systemName, subName), data));
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
				drops.add(components.get(axis).getItem());
			}
		}
		return drops;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public AnimationsPole getAnimator(){
		return animator;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public RenderPole getRenderer(){
		if(renderer == null){
			renderer = new RenderPole();
		}
		return renderer;
	}
	
	@Override
    public void save(WrapperNBT data){
		super.save(data);
		//Save all components.
		for(Entry<Axis, ATileEntityPole_Component> connectedObjectEntry : components.entrySet()){
			Axis axis = connectedObjectEntry.getKey();
			if(!axis.equals(Axis.NONE)){
				ATileEntityPole_Component component = connectedObjectEntry.getValue();
				WrapperNBT componentData = new WrapperNBT();
				component.save(componentData);
				data.setData(axis.name(), componentData);
			}
		}
	}
}
