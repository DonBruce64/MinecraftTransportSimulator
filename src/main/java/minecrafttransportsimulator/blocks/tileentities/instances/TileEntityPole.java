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
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.instances.AnimationsDecor;
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
	
	private static final AnimationsDecor animator = new AnimationsDecor();
	private static RenderPole renderer;
	
	private float maxTotalLightLevel;
	
	public TileEntityPole(WrapperWorld world, Point3d position, WrapperNBT data){
		super(world, position, data);
		//Load components back in.
		for(Axis axis : Axis.values()){
			WrapperNBT componentData = data.getData(axis.name());
			if(componentData != null){
				ATileEntityPole_Component newComponent = PoleComponentType.createComponent(this, componentData);
				components.put(axis, newComponent);
			}else if(axis.equals(Axis.NONE)){
				//Add our core component to the NONE axis.
				//This is done for ease of rendering and lookup routines.
				components.put(axis, PoleComponentType.createComponent(this, getItem().validateData(null)));
			}
		}
		
		//TODO remove legacy loader a few versions down the line.
		for(Axis axis : Axis.values()){
			String componentPackID = data.getString("packID" + axis.ordinal());
			if(!componentPackID.isEmpty()){
				String componentSystemName = data.getString("systemName" + axis.ordinal());
				String componentSubName = data.getString("subName" + axis.ordinal());
				ItemPoleComponent poleItem = PackParserSystem.getItem(componentPackID, componentSystemName, componentSubName);
				
				WrapperNBT fakeData = new WrapperNBT();
				fakeData.setString("packID", componentPackID);
				fakeData.setString("systemName", componentSystemName);
				fakeData.setString("subName", componentSubName);
				if(poleItem.definition.rendering != null && poleItem.definition.rendering.textObjects != null){
					for(int i=0; i<poleItem.definition.rendering.textObjects.size(); ++i){
						fakeData.setString("textLine" + i, data.getString("textLine" + axis.ordinal() + i));
					}
				}
				
				ATileEntityPole_Component newComponent = PoleComponentType.createComponent(this, fakeData);
				components.put(axis, newComponent);
			}
		}
	}
	
	/**
	 * Helper method to update light state and re-do world lighting if required.
	 */
	public void updateLightState(){
		float calculatedLevel = 0;
		for(ATileEntityPole_Component component : components.values()){
			calculatedLevel = Math.max(calculatedLevel, component.getLightProvided());
		}
		if(maxTotalLightLevel != calculatedLevel){
			world.updateLightBrightness(position);
		}
	}
	
	@Override
	public float getLightProvided(){
		return maxTotalLightLevel;
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
	public AnimationsDecor getAnimator(){
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
			WrapperNBT componentData = new WrapperNBT();
			connectedObjectEntry.getValue().save(componentData);
			data.setData(connectedObjectEntry.getKey().name(), componentData);
		}
	}
}
