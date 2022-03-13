package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

public class RenderPole extends ARenderEntityDefinable<TileEntityPole>{
	
	@Override
	public boolean disableRendering(TileEntityPole pole, float partialTicks){
		return true;
	}
}
