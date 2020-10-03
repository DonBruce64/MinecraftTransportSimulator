package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.baseclasses.Point3i;

/**Wrapper for blocks.  Unlike MC blocks, this class in instance-based.  In this
 * class, we have a reference to the block's position, hardness, and liquidity.
 * The idea is this class is used for collision and physics checks,
 * so it's more akin to BlockState than Block.
 *
 * @author don_bruce
 */
public interface IWrapperBlock{
	
	/**
	 *  Returns the position of this block in the world.
	 */
	public Point3i getPosition();
	
	/**
	 *  Returns the hardness of this block.
	 */
	public float getHardness();
	
	/**
	 *  Returns the slipperiness of this block.
	 */
	public float getSlipperiness();
	
	/**
	 *  Returns true if the block is liquid.
	 */
	public boolean isLiquid();
	
	/**
	 *  Returns true if it is raining on this block.
	 */
	public boolean isRaining();
}