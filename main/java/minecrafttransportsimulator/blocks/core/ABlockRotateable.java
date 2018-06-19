package minecrafttransportsimulator.blocks.core;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
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
	
public abstract class ABlockRotateable extends Block implements ITileEntityProvider{
	public static final PropertyDirection ROTATION = PropertyDirection.create("rotation");
	
    public ABlockRotateable(Material material){
		super(material);
		this.fullBlock = false;
		this.setDefaultState(this.blockState.getBaseState().withProperty(ROTATION, EnumFacing.SOUTH));
	}

	@Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack){
        super.onBlockPlacedBy(world, pos, state, entity, stack);
        ((ATileEntityRotatable) world.getTileEntity(pos)).rotation = (byte) entity.getHorizontalFacing().getOpposite().ordinal();
    }
	
	@Override
    public void breakBlock(World world, BlockPos pos, IBlockState state){
        super.breakBlock(world, pos, state);
        world.removeTileEntity(pos);
    }
    
	@Override
	public int getMetaFromState(IBlockState state){
        return 0;
    }
	
	@Override
	@SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess access, BlockPos pos){
		ATileEntityRotatable tile = (ATileEntityRotatable) access.getTileEntity(pos);
		if(tile != null && tile.rotation < EnumFacing.VALUES.length){
			return super.getActualState(state, access, pos).withProperty(ROTATION, EnumFacing.VALUES[tile.rotation]);
		}else{
			return super.getActualState(state, access, pos).withProperty(ROTATION, EnumFacing.SOUTH);
		}
    }
	
    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state){
        return false;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state){
        return false;
    }
    
	@Override
	public abstract ATileEntityRotatable createNewTileEntity(World worldIn, int meta);
    
    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, new IProperty[] {ROTATION});
    }
}
