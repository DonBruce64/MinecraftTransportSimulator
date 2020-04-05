package minecrafttransportsimulator.items.core;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import minecrafttransportsimulator.blocks.components.ABlockBase;

/**Interface that allows for this item to spawn an instance of {@link ABlockBase} into the world.
 * This interface doesn't actually spawn the item; rather, it allows such an item to be obtained.
 * 
 * @author don_bruce
 */
public interface IItemBlock{
	static final BiMap<IItemBlock, ABlockBase> itemToBlockMap = HashBiMap.create(); 
	
	/**
	 *  Creates the block that goes with this item.  This will only be called
	 *  ONCE during construction of the item.
	 */
	public ABlockBase createBlock();
	
	/**
	 *  Gets the block for this IItemBlock.
	 */
	public default ABlockBase getBlock(){
		if(!itemToBlockMap.containsKey(this)){
			itemToBlockMap.put(this, createBlock());
		}
		return itemToBlockMap.get(this);
	}
	
	/**
	 *  Gets the IItemBlock for the passed-in block.
	 */
	public static IItemBlock getItemForBlock(ABlockBase block){
		return itemToBlockMap.inverse().get(block);
	}
}
