package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.rendering.blocks.ARenderTileEntityBase;
import minecrafttransportsimulator.rendering.blocks.RenderStreetlight;
import minecrafttransportsimulator.wrappers.WrapperNBT;

/**Streetlight tile entity.  Just a simple light to make the city bright!
 * 
 * @author don_bruce
 */
public class TileEntityPoleStreetlight extends ATileEntityBase{
	
	@Override
	public void load(WrapperNBT data){}

	@Override
	public void save(WrapperNBT data){}

	@Override
	public ARenderTileEntityBase<? extends ATileEntityBase, ? extends IBlockTileEntity> getRenderer(){
		return new RenderStreetlight();
	}
}
