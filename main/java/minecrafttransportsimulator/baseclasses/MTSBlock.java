package minecrafttransportsimulator.baseclasses;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public abstract class MTSBlock extends Block{

	public MTSBlock(Material material, float hardness, float resistance){
		super(material);
		this.setHardness(hardness);
		this.setResistance(resistance);
		this.setDefaultBlockBounds();
	}
		
	protected abstract boolean isBlock3D();
		
	protected abstract void setDefaultBlockBounds();
	
	protected abstract void setBlockBoundsFromMetadata(IBlockState state);

	public void setBlockMetadata(World world, BlockPos pos, IBlockState blockState){
		world.setBlockState(pos, blockState);
	}

	@Override
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB box, List<AxisAlignedBB> collidingBoxes, Entity entity) {
		this.setBlockBoundsFromMetadata(world.getBlockState(pos));
		super.addCollisionBoxToList(state, world, pos, box, collidingBoxes, entity);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
		this.setBlockBoundsFromMetadata(world.getBlockState(pos));
		return super.getSelectedBoundingBox(state, world, pos);
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return isBlock3D() ? EnumBlockRenderType.INVISIBLE : super.getRenderType(state);
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return !isBlock3D();
	}

}
