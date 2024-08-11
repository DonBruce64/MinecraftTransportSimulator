package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockDecor extends ABlockBaseTileEntity {

    public BlockDecor() {
        super(10.0F, 5.0F);
    }

    @Override
    public Class<TileEntityDecor> getTileEntityClass() {
        return TileEntityDecor.class;
    }

    @Override
    public TileEntityDecor createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, AItemSubTyped<?> item, IWrapperNBT data) {
        return new TileEntityDecor(world, position, placingPlayer, (ItemDecor) item, data);
    }
}
