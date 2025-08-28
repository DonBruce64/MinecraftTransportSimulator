package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityItemLoader;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockItemLoader extends ABlockBaseTileEntity {

    public BlockItemLoader() {
        super(DEFAULT_HARDNESS, DEFAULT_BLAST_RESISTANCE);
    }

    @Override
    public Class<TileEntityItemLoader> getTileEntityClass() {
        return TileEntityItemLoader.class;
    }

    @Override
    public TileEntityItemLoader createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, AItemSubTyped<?> item, IWrapperNBT data) {
        return new TileEntityItemLoader(world, position, placingPlayer, (ItemDecor) item, data);
    }
}
