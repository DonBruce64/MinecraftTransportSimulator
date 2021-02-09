package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.jsondefs.JSONSound;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.sound.Radio;

/**Radio tile entity.  Contains saved radio data and is responsible
 * for handling all radio calls to the block.
 *
 * @author don_bruce
 */
public class TileEntityRadio extends TileEntityDecor{
	//Internal radio variables.
	private final Radio radio;
	private final Point3d soundVelocity = new Point3d();
	
	public TileEntityRadio(WrapperWorld world, Point3i position, WrapperNBT data){
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
	
	@Override
	public boolean isProviderValid(){
		return false;
	}
	
	@Override
	public List<JSONSound> getSoundDefinitions(){
		return definition.rendering != null ? definition.rendering.sounds : null;
	}

	@Override
	public Point3d getProviderVelocity(){
		return soundVelocity;
	}

	@Override
	public Radio getRadio(){
		return radio;
	}
}
