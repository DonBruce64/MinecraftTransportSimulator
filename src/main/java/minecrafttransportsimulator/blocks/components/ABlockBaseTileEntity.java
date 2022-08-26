package minecrafttransportsimulator.blocks.components;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * Abstract class for blocks that have tile entities.  Such tile entities
 * are pack-based, so are linked to a specific pack definition.
 *
 * @author don_bruce
 */
public abstract class ABlockBaseTileEntity extends ABlockBase {

    public ABlockBaseTileEntity(float hardness, float blastResistance) {
        super(hardness, blastResistance);
    }

    @Override
    public void onBroken(AWrapperWorld world, Point3D position) {
        ATileEntityBase<?> tile = world.getTileEntity(position);
        if (tile != null) {
            tile.destroy(tile.boundingBox);
        }
    }

    /**
     * Gets the class that this block will create a TileEntity from.
     * This is used during registration to figure out which TE goes
     * to which block, and how to MC interface information when creating
     * the MC Builder that will contain the TileEntity for this block.
     * this is because the builder gets created before the main tile.
     */
    public abstract Class<? extends ATileEntityBase<?>> getTileEntityClass();

    /**
     * Gets a new Tile Entity for this block.
     * The placingPlayer may be null if this is being loaded from NBT.
     */
    public abstract ATileEntityBase<?> createTileEntity(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data);
}