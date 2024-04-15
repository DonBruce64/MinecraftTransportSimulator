package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class BlockRoad extends ABlockBaseTileEntity {

    public BlockRoad() {
        super(10.0F, 5.0F);
    }

    @Override
    public Class<TileEntityRoad> getTileEntityClass() {
        return TileEntityRoad.class;
    }

    @Override
    public TileEntityRoad createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, AItemSubTyped<?> item, IWrapperNBT data) {
        return new TileEntityRoad(world, position, placingPlayer, (ItemRoadComponent) item, data);
    }
}
