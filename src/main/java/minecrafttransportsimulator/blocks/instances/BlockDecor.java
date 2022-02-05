package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockDecor extends ABlockBaseTileEntity{
	
    public BlockDecor(){
    	super(10.0F, 5.0F);
	}
    
    @Override
    public Class<TileEntityDecor> getTileEntityClass(){
    	return TileEntityDecor.class;
    }
    
    @Override
	public TileEntityDecor createTileEntity(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		return new TileEntityDecor(world, position, placingPlayer, data);
	}
}
