package mcinterface1122;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.mcinterface.IWrapperBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

class WrapperBlock implements IWrapperBlock{
	final Point3i position;
	final float hardness;
	final float slipperiness;
	final boolean isLiquid;
	final boolean isRaining;

	WrapperBlock(World world, BlockPos pos){
		IBlockState state = world.getBlockState(pos);
		this.position = new Point3i(pos.getX(), pos.getY(), pos.getZ());
		this.hardness = state.getBlockHardness(world, pos);
		this.slipperiness = state.getBlock().getSlipperiness(state, world, pos, null);
		this.isLiquid = state.getMaterial().isLiquid();
		this.isRaining = world.isRainingAt(pos.up());
	}
	
	@Override
	public Point3i getPosition(){
		return position;
	}
	
	@Override
	public float getHardness(){
		return hardness;
	}
	
	@Override
	public float getSlipperiness(){
		return slipperiness;
	}
	
	@Override
	public boolean isLiquid(){
		return isLiquid;
	}
	
	@Override
	public boolean isRaining(){
		return isRaining;
	}
}