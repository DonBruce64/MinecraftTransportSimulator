package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.sound.Radio;

/**Radio tile entity.  Contains saved radio data and is responsible
 * for handling all radio calls to the block.
 *
 * @author don_bruce
 */
public class TileEntityRadio extends TileEntityDecor{
	//Internal radio variable.
	public final Radio radio;
	
	public TileEntityRadio(WrapperWorld world, Point3d position, WrapperNBT data){
		super(world, position, data);
		this.radio = new Radio(this, data);
	}
	
	@Override
	public void remove(){
		super.remove();
		radio.stop();
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		radio.save(data);
	}
}
