package minecrafttransportsimulator.rendering.blocks;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Core;

public class RenderPole extends ARenderTileEntityBase<TileEntityPole, BlockPole>{
	
	@Override
	public void render(TileEntityPole tile, BlockPole block, float partialTicks){
		//First render all connections.
		//These are based on the pole itself, so we first need to get the pole.
		TileEntityPole_Core coreComponent = (TileEntityPole_Core) tile.components.get(Axis.NONE);
		if(coreComponent != null){
			//Render the connected component, but only if the solid component isn't being called.
			for(Axis axis : Axis.values()){
				if(tile.components.containsKey(axis)){
					
				}
			}
		}
	}
}
