package mcinterface;

import minecrafttransportsimulator.baseclasses.Point3i;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**Wrapper for blocks.  Unlike MC blocks, this class in instance-based.  In this
 * class, we have a reference to the block's position, hardness, and liquidity.
 * The idea is this class is used for collision and physics checks,
 * so it's more akin to BlockState than Block.
 *
 * @author don_bruce
 */
public class WrapperBlock{
	final Point3i position;
	final float hardness;
	final float slipperiness;
	final boolean isLiquid;
	final boolean isRaining;

	public WrapperBlock(World world, BlockPos pos){
		IBlockState state = world.getBlockState(pos);
		this.position = new Point3i(pos.getX(), pos.getY(), pos.getZ());
		this.hardness = state.getBlockHardness(world, pos);
		this.slipperiness = state.getBlock().getSlipperiness(state, world, pos, null);
		this.isLiquid = state.getMaterial().isLiquid();
		this.isRaining = world.isRainingAt(pos.up());
	}
	
	/**
	 *  Returns the position of this block in the world.
	 */
	public Point3i getPosition(){
		return position;
	}
	
	/**
	 *  Returns the hardness of this block.
	 */
	public float getHardness(){
		return hardness;
	}
	
	/**
	 *  Returns the slipperiness of this block.
	 */
	public float getSlipperiness(){
		return slipperiness;
	}
	
	/**
	 *  Returns true if the block is liquid.
	 */
	public boolean isLiquid(){
		return isLiquid;
	}
	
	/**
	 *  Returns true if it is raining on this block.
	 */
	public boolean isRaining(){
		return isRaining;
	}
}