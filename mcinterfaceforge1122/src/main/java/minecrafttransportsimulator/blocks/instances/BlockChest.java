package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityChest;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockChest extends ABlockBaseTileEntity {

    public BlockChest() {
        super(10.0F, 5.0F);
    }

    @Override
    public Class<TileEntityChest> getTileEntityClass() {
        return TileEntityChest.class;
    }

    @Override
    public TileEntityChest createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        return new TileEntityChest(world, position, placingPlayer, data);
    }
}
