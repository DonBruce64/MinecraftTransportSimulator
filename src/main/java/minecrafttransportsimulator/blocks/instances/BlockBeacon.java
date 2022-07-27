package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityBeacon;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockBeacon extends ABlockBaseTileEntity {

    public BlockBeacon() {
        super(10.0F, 5.0F);
    }

    @Override
    public Class<TileEntityBeacon> getTileEntityClass() {
        return TileEntityBeacon.class;
    }

    @Override
    public TileEntityBeacon createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        return new TileEntityBeacon(world, position, placingPlayer, data);
    }
}
