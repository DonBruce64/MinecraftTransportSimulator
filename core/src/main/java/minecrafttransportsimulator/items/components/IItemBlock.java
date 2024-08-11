package minecrafttransportsimulator.items.components;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * Interface that allows for this item to spawn an instance of {@link ABlockBase} into the world.
 * This interface doesn't actually spawn the item; rather, it allows such an item to be obtained.
 *
 * @author don_bruce
 */
public interface IItemBlock {
    Map<IItemBlock, ABlockBase> itemToBlockMap = new HashMap<>();

    /**
     * Returns the block class that goes to this IItemBlock.
     * Used to create an instance of the block, but functions
     * as a key to prevent creating gobs of block instances
     * that we just throw away after registration.
     */
    Class<? extends ABlockBase> getBlockClass();

    /**
     * Gets the block for this IItemBlock.
     */
    default ABlockBase getBlock() {
        if (!itemToBlockMap.containsKey(this)) {
            //First check to see if we already created the block class.
            Class<? extends ABlockBase> blockClass = getBlockClass();
            ABlockBase existingBlock = null;
            for (ABlockBase block : itemToBlockMap.values()) {
                if (blockClass.equals(block.getClass())) {
                    existingBlock = block;
                    break;
                }
            }

            //If we have an existing block, put it in the map.
            //Otherwise, make a new one.
            if (existingBlock != null) {
                itemToBlockMap.put(this, existingBlock);
            } else {
                try {
                    itemToBlockMap.put(this, getBlockClass().newInstance());
                } catch (Exception e) {
                    //Uh oh....
                    e.printStackTrace();
                }
            }
        }
        return itemToBlockMap.get(this);
    }

    /**
     * Tries to let this player place the block for this IItemBlock into the world.
     * Returns true if the block was placed.
     */
    default boolean placeBlock(AWrapperWorld world, IWrapperPlayer player, Point3D position, Axis axis) {
        return world.setBlock(getBlock(), position, player, axis);
    }
}
