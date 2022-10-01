package minecrafttransportsimulator.blocks.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Slightly-less basic block class.  This class is used for collision operations where a non-standard collision is required.
 * Mainly used on roads, but could be used on other things in the future.
 *
 * @author don_bruce
 */
public class BlockCollision extends ABlockBase {
    public static List<BlockCollision> blockInstances = createCollisionBlocks();
    public final BoundingBox blockBounds;

    public BlockCollision(int collisionHeightInPixels) {
        super(10.0F, 5.0F);
        if (collisionHeightInPixels == 0) {
            collisionHeightInPixels = 1;
        }
        float heightRadiusRequired = collisionHeightInPixels / 16F / 2F;
        //Need to offset by 0.5 to center ourselves in the block.
        this.blockBounds = new BoundingBox(new Point3D(0.5, heightRadiusRequired, 0.5), 0.5D, heightRadiusRequired, 0.5D);
    }

    @Override
    public void onBroken(AWrapperWorld world, Point3D position) {
        TileEntityRoad masterBlock = getMasterRoad(world, position);
        if (masterBlock != null && masterBlock.isActive()) {
            //We belong to this TE.  Destroy the block.  This will end up
            //destroying all collisions, including this one.  However, since
            //we check if the road block is isActive, and that gets set before destroying
            //all collision blocks, the recursive call won't make it down here.
            world.destroyBlock(masterBlock.position, true);
        }
    }

    /**
     * Helper method to get the master road instance given the position of a block in the world.
     * This is made non-static simply to ensure people obtain a reference to an actual collision block
     * prior to trying to call this method, as there aren't any bound-able checks we can do on the two
     * input variables.
     */
    public TileEntityRoad getMasterRoad(AWrapperWorld world, Point3D position) {
        Point3D blockOffset = new Point3D();
        Point3D testPoint = new Point3D();
        //Search XZ before Y, as most master roads are on the same Y-level as the collision block.
        for (int j = -ConfigSystem.settings.general.roadMaxLength.value; j < 2 * ConfigSystem.settings.general.roadMaxLength.value; ++j) {
            for (int i = -ConfigSystem.settings.general.roadMaxLength.value; i < 2 * ConfigSystem.settings.general.roadMaxLength.value; ++i) {
                for (int k = -ConfigSystem.settings.general.roadMaxLength.value; k < 2 * ConfigSystem.settings.general.roadMaxLength.value; ++k) {
                    blockOffset.set(i, j, k);
                    testPoint.set(position);
                    testPoint.subtract(blockOffset);
                    ATileEntityBase<?> testTile = world.getTileEntity(testPoint);
                    if (testTile instanceof TileEntityRoad) {
                        if (((TileEntityRoad) testTile).collisionBlockOffsets.contains(blockOffset)) {
                            return (TileEntityRoad) testTile;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static List<BlockCollision> createCollisionBlocks() {
        List<BlockCollision> blocks = new ArrayList<>();
        for (int i = 0; i < 16; ++i) {
            blocks.add(new BlockCollision(i));
        }
        return blocks;
    }
}
