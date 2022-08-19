package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFluidLoader;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockFluidLoader extends ABlockBaseTileEntity {

    public BlockFluidLoader() {
        super(10.0F, 5.0F);
    }

    @Override
    public Class<TileEntityFluidLoader> getTileEntityClass() {
        return TileEntityFluidLoader.class;
    }

    @Override
    public TileEntityFluidLoader createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        return new TileEntityFluidLoader(world, position, placingPlayer, data);
    }
}
