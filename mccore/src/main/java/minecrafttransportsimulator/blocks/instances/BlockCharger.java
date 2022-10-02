package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityCharger;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockCharger extends ABlockBaseTileEntity {

    public BlockCharger() {
        super(10.0F, 5.0F);
    }

    @Override
    public Class<TileEntityCharger> getTileEntityClass() {
        return TileEntityCharger.class;
    }

    @Override
    public TileEntityCharger createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        return new TileEntityCharger(world, position, placingPlayer, data);
    }
}
