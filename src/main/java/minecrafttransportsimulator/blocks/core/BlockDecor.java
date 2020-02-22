package minecrafttransportsimulator.blocks.core;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.ItemDecor;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockDecor extends ABlockRotatable implements ITileEntityProvider{
	private final boolean isOriented;
	
	private AxisAlignedBB regularAABB;
	private AxisAlignedBB rotatedAABB;
	
    public BlockDecor(boolean isOriented, boolean lighted){
		super();
		this.isOriented = isOriented;
		this.fullBlock = false;
		this.setLightLevel(lighted ? 1.0F : 0);
	}
    
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state){
        super.breakBlock(world, pos, state);
        world.removeTileEntity(pos);
    }
    
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack){
    	TileEntityDecor decorTile = ((TileEntityDecor) world.getTileEntity(pos));
    	decorTile.definition = ((ItemDecor) stack.getItem()).definition;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess access, BlockPos pos){
    	if(!isOriented){
    		if(regularAABB == null){
    			TileEntityDecor tile = ((TileEntityDecor) access.getTileEntity(pos));
    			if(tile != null && tile.definition != null){
		    		regularAABB = new AxisAlignedBB(0.5F - tile.definition.general.width/2F, 0, 0.5F - tile.definition.general.depth/2F, 0.5F + tile.definition.general.width/2F, tile.definition.general.height, 0.5F +  tile.definition.general.depth/2F);
    			}
    		}else{
    			return regularAABB;
    		}
    	}else{
    		if(regularAABB == null){
    			TileEntityDecor tile = ((TileEntityDecor) access.getTileEntity(pos));
    			if(tile != null && tile.definition != null){
					regularAABB = new AxisAlignedBB(0.5F - tile.definition.general.width/2F, 0, 0.5F - tile.definition.general.depth/2F, 0.5F + tile.definition.general.width/2F, tile.definition.general.height, 0.5F +  tile.definition.general.depth/2F);
					rotatedAABB = !isOriented ? regularAABB : new AxisAlignedBB(0.5F - tile.definition.general.depth/2F, 0, 0.5F - tile.definition.general.width/2F, 0.5F + tile.definition.general.depth/2F, tile.definition.general.height, 0.5F +  tile.definition.general.width/2F);
    			}
    		}else{
    			return state.getValue(FACING).getAxis().equals(EnumFacing.Axis.Z) ? regularAABB : rotatedAABB;
    		}
    	}
        return super.getBoundingBox(state, access, pos);
    }
    
	@Override
	@SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean p_185477_7_){
		if(!isOriented){
			addCollisionBoxToList(pos, entityBox, collidingBoxes, FULL_BLOCK_AABB);
		}else{
			if(regularAABB == null){
    			TileEntityDecor tile = ((TileEntityDecor) world.getTileEntity(pos));
    			if(tile != null && tile.definition != null){
					regularAABB = new AxisAlignedBB(0.5F - tile.definition.general.width/2F, 0, 0.5F - tile.definition.general.depth/2F, 0.5F + tile.definition.general.width/2F, tile.definition.general.height, 0.5F +  tile.definition.general.depth/2F);
					rotatedAABB = !isOriented ? regularAABB : new AxisAlignedBB(0.5F - tile.definition.general.depth/2F, 0, 0.5F - tile.definition.general.width/2F, 0.5F + tile.definition.general.depth/2F, tile.definition.general.height, 0.5F +  tile.definition.general.width/2F);
    			}
    			addCollisionBoxToList(pos, entityBox, collidingBoxes, FULL_BLOCK_AABB);
    		}else{
    			addCollisionBoxToList(pos, entityBox, collidingBoxes, state.getValue(FACING).getAxis().equals(EnumFacing.Axis.Z) ? regularAABB : rotatedAABB);
    		}
		}
    }
	
	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player){
		TileEntityDecor tile = ((TileEntityDecor) world.getTileEntity(pos));
		if(tile.definition != null){
			return new ItemStack(MTSRegistry.packItemMap.get(tile.definition.packID).get(tile.definition.systemName));
		}else{
			return ItemStack.EMPTY;
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
    @SuppressWarnings("deprecation")
    public EnumBlockRenderType getRenderType(IBlockState state){
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }
    
	@Override
	public TileEntityDecor createNewTileEntity(World worldIn, int meta){
		return new TileEntityDecor();
	}
}
