package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRadio;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockRadio extends ABlockBaseTileEntity{
	
	public BlockRadio(){
		super(10.0F, 5.0F);
	}
	
	@Override
    public Class<TileEntityRadio> getTileEntityClass(){
    	return TileEntityRadio.class;
    }

	@Override
	public TileEntityRadio createTileEntity(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		return new TileEntityRadio(world, position, placingPlayer, data);
	}
}
