package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.items.instances.ItemBlock;

/**
 * Standard block class for basic blocks.  Takes in a JSON definition to create.
 *
 * @author don_bruce
 */
public class BlockBlock extends ABlockBase {
    public final ItemBlock itemReference;

    public BlockBlock(ItemBlock itemReference) {
        super(DEFAULT_HARDNESS, DEFAULT_BLAST_RESISTANCE);

        this.itemReference = itemReference;
    }
}
