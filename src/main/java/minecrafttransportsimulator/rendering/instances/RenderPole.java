package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;

public class RenderPole extends ARenderTileEntityBase<TileEntityPole>{
	
	@Override
	public void renderModel(TileEntityPole pole, float partialTicks){
		//Don't render anything here.  Components and connectors get rendered in their own routines.
	}
	
	@Override
	protected void renderSupplementalModels(TileEntityPole pole, float partialTicks){
		for(Axis axis : Axis.values()){
			if(pole.components.containsKey(axis)){
				ATileEntityPole_Component component = pole.components.get(axis);
				component.getRenderer().render(component, partialTicks);
			}
		}
	}
	
	@Override
	public void clearObjectCaches(TileEntityPole pole){
		super.clearObjectCaches(pole);
		for(Axis axis : Axis.values()){
			if(!axis.equals(Axis.NONE)){
				if(pole.components.containsKey(axis)){
					ATileEntityPole_Component component = pole.components.get(axis);
					component.getRenderer().clearObjectCaches(component);
				}
			}
		}
	}
	
	@Override
	public boolean rotateToBlock(){
		return false;
	}
	
	@Override
	public boolean translateToSlabs(){
		return false;
	}
}
