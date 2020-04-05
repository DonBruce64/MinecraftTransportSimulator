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
	
	public WrapperTileEntityTickable(){
		//Blank constructor for MC.  We set the TE variable in NBT instead.
	}
	
	WrapperTileEntityTickable(ATileEntityTickable tileEntity){
		super(tileEntity);
	}

	@Override
    public void update(){
		((ATileEntityTickable) tileEntity).update();
    }
}
