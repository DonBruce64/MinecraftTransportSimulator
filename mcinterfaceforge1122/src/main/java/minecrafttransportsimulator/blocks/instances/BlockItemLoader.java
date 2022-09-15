package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityItemLoader;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockItemLoader extends ABlockBaseTileEntity {

    public BlockItemLoader() {
        super(10.0F, 5.0F);
    }

    @Override
    public Class<TileEntityItemLoader> getTileEntityClass() {
        return TileEntityItemLoader.class;
    }

    @Override
    public TileEntityItemLoader createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        return new TileEntityItemLoader(world, position, placingPlayer, data);
    }
}
