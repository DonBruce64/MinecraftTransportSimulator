package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockFuelPump extends ABlockBaseTileEntity {

    public BlockFuelPump() {
        super(10.0F, 5.0F);
    }

    @Override
    public Class<TileEntityFuelPump> getTileEntityClass() {
        return TileEntityFuelPump.class;
    }

    @Override
    public TileEntityFuelPump createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        return new TileEntityFuelPump(world, position, placingPlayer, data);
    }
}
