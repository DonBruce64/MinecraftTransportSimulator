package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityBeacon;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockBeacon extends ABlockBaseTileEntity {

    public BlockBeacon() {
        super(DEFAULT_HARDNESS, DEFAULT_BLAST_RESISTANCE);
    }

    @Override
    public Class<TileEntityBeacon> getTileEntityClass() {
        return TileEntityBeacon.class;
    }

    @Override
    public TileEntityBeacon createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, AItemSubTyped<?> item, IWrapperNBT data) {
        return new TileEntityBeacon(world, position, placingPlayer, (ItemDecor) item, data);
    }
}
