package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.rendering.blocks.ARenderTileEntityBase;
import minecrafttransportsimulator.rendering.blocks.RenderDecor;
import minecrafttransportsimulator.wrappers.WrapperNBT;
import minecrafttransportsimulator.wrappers.WrapperTileEntity;

/**Decor tile entity.  Does nothing.  Only here to let us do a TESR.
 *
 * @author don_bruce
 */
public class TileEntityDecor extends ATileEntityBase{
	
	@Override
	public void load(WrapperNBT data){}

	@Override
	public void save(WrapperNBT data){}

	@Override
	public ARenderTileEntityBase<? extends ATileEntityBase, ? extends WrapperTileEntity.IProvider> getRenderer(){
		return new RenderDecor();
	}
}
