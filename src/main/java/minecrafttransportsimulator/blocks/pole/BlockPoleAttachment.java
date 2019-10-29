package minecrafttransportsimulator.blocks.pole;

import minecrafttransportsimulator.blocks.core.TileEntityRotatable;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockPoleAttachment extends BlockPoleNormal implements ITileEntityProvider{
	public static final PropertyDirection ROTATION = PropertyDirection.create("rotation");
    
	public BlockPoleAttachment(float poleRadius){
		super(poleRadius);
	}
	
	@Override
    public boolean canConnectOnSide(IBlockAccess access, BlockPos pos, EnumFacing side){
		TileEntityRotatable tile = (TileEntityRotatable) access.getTileEntity(pos);
		if(tile != null){
			return !side.equals(EnumFacing.VALUES[tile.rotation]);
		}else{
			return false;
		}
    }
	
	@Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack){
        super.onBlockPlacedBy(world, pos, state, entity, stack);
        ((TileEntityRotatable) world.getTileEntity(pos)).rotation = (byte) entity.getHorizontalFacing().getOpposite().ordinal();
    }
	
	@Override
    public void breakBlock(World world, BlockPos pos, IBlockState state){
        super.breakBlock(world, pos, state);
        world.removeTileEntity(pos);
    }
	
	@Override
	@SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess access, BlockPos pos){
		TileEntityRotatable tile = (TileEntityRotatable) access.getTileEntity(pos);
		if(tile != null){
			return super.getActualState(state, access, pos).withProperty(ROTATION, EnumFacing.VALUES[tile.rotation]);
		}else{
			return super.getActualState(state, access, pos).withProperty(ROTATION, EnumFacing.SOUTH);
		}
    }
	
	@Override
	public TileEntityRotatable createNewTileEntity(World worldIn, int meta){
		return new TileEntityPoleWallConnector();
	}
	
    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, new IProperty[] {UP, DOWN, NORTH, EAST, SOUTH, WEST, ROTATION});
    }
}
