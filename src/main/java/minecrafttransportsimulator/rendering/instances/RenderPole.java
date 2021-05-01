package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;

public class RenderPole extends ARenderTileEntityBase<TileEntityPole>{
	
	@Override
	public boolean disableMainRendering(TileEntityPole pole, float partialTicks){
		return true;
	}
	
	@Override
	protected void renderSupplementalModels(TileEntityPole pole, boolean blendingEnabled, float partialTicks){
		for(Axis axis : Axis.values()){
			if(pole.components.containsKey(axis)){
				ATileEntityPole_Component component = pole.components.get(axis);
				component.getRenderer().render(component, blendingEnabled, partialTicks);
			}
		}
	}
	
	@Override
	public boolean translateToSlabs(){
		return false;
	}
}
