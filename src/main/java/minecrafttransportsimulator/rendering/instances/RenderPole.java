package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;

public class RenderPole extends ARenderTileEntityBase<TileEntityPole>{
	
	@Override
	public boolean disableRendering(TileEntityPole pole, float partialTicks){
		return true;
	}
	
	@Override
	public boolean translateToSlabs(){
		return false;
	}
}
