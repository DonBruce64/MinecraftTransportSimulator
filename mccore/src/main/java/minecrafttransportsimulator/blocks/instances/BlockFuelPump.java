package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockFuelPump extends ABlockBaseTileEntity {

    public BlockFuelPump() {
        super(DEFAULT_HARDNESS, DEFAULT_BLAST_RESISTANCE);
    }

    @Override
    public Class<TileEntityFuelPump> getTileEntityClass() {
        return TileEntityFuelPump.class;
    }

    @Override
    public TileEntityFuelPump createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, AItemSubTyped<?> item, IWrapperNBT data) {
        return new TileEntityFuelPump(world, position, placingPlayer, (ItemDecor) item, data);
    }
}
