package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockSignalController extends ABlockBaseTileEntity {

    public BlockSignalController() {
        super(10.0F, 5.0F);
    }

    @Override
    public Class<TileEntitySignalController> getTileEntityClass() {
        return TileEntitySignalController.class;
    }

    @Override
    public TileEntitySignalController createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        return new TileEntitySignalController(world, position, placingPlayer, data);
    }
}
