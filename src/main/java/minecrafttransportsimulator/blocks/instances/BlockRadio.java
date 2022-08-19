package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRadio;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockRadio extends ABlockBaseTileEntity {

    public BlockRadio() {
        super(10.0F, 5.0F);
    }

    @Override
    public Class<TileEntityRadio> getTileEntityClass() {
        return TileEntityRadio.class;
    }

    @Override
    public TileEntityRadio createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        return new TileEntityRadio(world, position, placingPlayer, data);
    }
}
