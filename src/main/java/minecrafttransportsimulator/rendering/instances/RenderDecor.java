package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;

public class RenderDecor extends ARenderTileEntityBase<TileEntityDecor>{
	
	@Override
	public String getTexture(TileEntityDecor decor){
		return decor.definition.getTextureLocation(decor.subName);
	}
}
