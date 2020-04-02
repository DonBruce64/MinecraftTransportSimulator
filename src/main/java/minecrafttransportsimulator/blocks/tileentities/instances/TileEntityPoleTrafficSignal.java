package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.rendering.blocks.ARenderTileEntityBase;
import minecrafttransportsimulator.rendering.blocks.RenderTrafficSignal;
import minecrafttransportsimulator.wrappers.WrapperNBT;
import minecrafttransportsimulator.wrappers.WrapperTileEntity;

/**Traffic signal tile entity.  This doesn't tick, as the state of the light
 * is by default a flashing red until changed by a {@link TileEntityTrafficSignalController}.
 * 
 * @author don_bruce
 */
public class TileEntityPoleTrafficSignal extends ATileEntityBase{
	public SignalState state = SignalState.FLASHING_RED;
	
	@Override
	public void load(WrapperNBT data){}

	@Override
	public void save(WrapperNBT data){}
	
	@Override
	public ARenderTileEntityBase<? extends ATileEntityBase, ? extends WrapperTileEntity.IProvider> getRenderer(){
		return new RenderTrafficSignal();
	}

	public static enum SignalState{
		OFF,
		GREEN,
		YELLOW,
		RED,
		FLASHING_YELLOW,
		FLASHING_RED;
	}
}
