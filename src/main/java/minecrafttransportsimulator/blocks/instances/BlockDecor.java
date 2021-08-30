package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockDecor extends ABlockBaseDecor{
	
    public BlockDecor(){
    	super();
	}
    
    @Override
	public TileEntityDecor createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityDecor(world, position, data);
	}
}
