package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityItemLoader;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockItemLoader extends ABlockBaseTileEntity{
	
	public BlockItemLoader(){
		super(10.0F, 5.0F);
	}
	
	@Override
    public Class<TileEntityItemLoader> getTileEntityClass(){
    	return TileEntityItemLoader.class;
    }
	
    @Override
	public TileEntityItemLoader createTileEntity(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		return new TileEntityItemLoader(world, position, placingPlayer, data);
	}
}
