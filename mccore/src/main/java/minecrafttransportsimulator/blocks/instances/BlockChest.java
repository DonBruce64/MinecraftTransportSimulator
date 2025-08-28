package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityChest;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockChest extends ABlockBaseTileEntity {

    public BlockChest() {
        super(DEFAULT_HARDNESS, DEFAULT_BLAST_RESISTANCE);
    }

    @Override
    public Class<TileEntityChest> getTileEntityClass() {
        return TileEntityChest.class;
    }

    @Override
    public TileEntityChest createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, AItemSubTyped<?> item, IWrapperNBT data) {
        return new TileEntityChest(world, position, placingPlayer, (ItemDecor) item, data);
    }
}
