package minecrafttransportsimulator.wrappers;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityTickable;
import net.minecraft.util.ITickable;

/**Simply a wrapper for tickable tile entities.  All this adds is an update()
 * call for updating the Tile Entity every tick.  Try not to make things tick
 * if you don't have to, okay?
 *
 * @author don_bruce
 */
public class WrapperTileEntityTickable extends WrapperTileEntity implements ITickable{
	
	WrapperTileEntityTickable(ATileEntityTickable tileEntity){
		super(tileEntity);
	}

	@Override
    public void update(){
		((ATileEntityTickable) tileEntity).update();
    }
	
	/**Interface that tells the system this block should create an instance of a {@link ATileEntityTickable} when created.
	*
	* @author don_bruce
	*/
	public static interface IProvider extends WrapperTileEntity.IProvider{
		
		@Override
		public abstract ATileEntityTickable createTileEntity();
	}
}
