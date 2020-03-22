package minecrafttransportsimulator.blocks.pole;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockPoleAttachment extends BlockPoleNormal implements ITileEntityProvider{
	public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    
	public BlockPoleAttachment(float poleRadius){
		super(poleRadius);
		this.setDefaultState(this.getDefaultState().withProperty(FACING, EnumFacing.SOUTH));
	}
	
	@Override
    public boolean canConnectOnSide(IBlockAccess access, BlockPos pos, EnumFacing side){
		return !access.getBlockState(pos).getValue(FACING).equals(side);
    }
	
	@Override
	@SuppressWarnings("deprecation")
	public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer){
        return super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer).withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }
	
	@Override
    public void breakBlock(World world, BlockPos pos, IBlockState state){
        super.breakBlock(world, pos, state);
        world.removeTileEntity(pos);
    }
	
	@Override
	@SuppressWarnings("deprecation")
	public IBlockState getStateFromMeta(int meta){
        return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state){
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rot){
        return state.getBlock() != this ? state : state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockStateContainer createBlockState(){
    	return new BlockStateContainer(this, new IProperty[] {UP, DOWN, NORTH, EAST, SOUTH, WEST, FACING});
    }
	
	@Override
	public TileEntityPoleAttachment createNewTileEntity(World worldIn, int meta){
		return new TileEntityPoleAttachment();
	}
}
