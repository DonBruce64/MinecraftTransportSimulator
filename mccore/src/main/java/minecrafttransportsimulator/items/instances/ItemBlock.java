package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockBlock;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.jsondefs.JSONBlock;

/**
 * Block item.  This spawns a block into the world.  Orientation is handled
 * by MC to allow for structures.  This class is just here as a property holder.
 *
 * @author don_bruce
 */
public class ItemBlock extends AItemPack<JSONBlock> implements IItemBlock {

    public ItemBlock(JSONBlock definition) {
        super(definition, null);
    }

    @Override
    public Class<? extends ABlockBase> getBlockClass() {
        //Return null, since we need a new class instance for each defined block.
        return null;
    }

    @Override
    public ABlockBase createBlock() {
        return new BlockBlock(this);
    }
}
