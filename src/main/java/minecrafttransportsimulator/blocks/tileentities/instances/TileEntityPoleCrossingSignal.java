package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.rendering.blocks.ARenderTileEntityBase;
import minecrafttransportsimulator.rendering.blocks.RenderCrossingSignal;
import minecrafttransportsimulator.wrappers.WrapperNBT;
import minecrafttransportsimulator.wrappers.WrapperTileEntity.IProvider;

/**Crossing signal tile entity.  This doesn't tick, as the state of the signal
 * is by default a flashing nowalk hand until changed by a {@link TileEntityTrafficSignalController}.
 * 
 * @author don_bruce
 */
public class TileEntityPoleCrossingSignal extends ATileEntityBase{
	public CrossingState state = CrossingState.FLASHING_DONTWALK;
	
	@Override
	public void load(WrapperNBT data){}

	@Override
	public void save(WrapperNBT data){}
	

	public static enum CrossingState{
		OFF,
		WALK,
		FLASHING_DONTWALK,
		DONTWALK;
	}

	@Override
	public ARenderTileEntityBase<? extends ATileEntityBase, ? extends IProvider> getRenderer(){
		return new RenderCrossingSignal();
	}
}
