package minecrafttransportsimulator.blocks.core;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.dataclasses.PackDecorObject;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockDecor extends Block implements ITileEntityProvider{
	private final boolean isOriented;
	
	private AxisAlignedBB regularAABB;
	private AxisAlignedBB rotatedAABB;
	
    public BlockDecor(boolean isOriented, boolean lighted){
		super(Material.ROCK);
		this.isOriented = isOriented;
		this.fullBlock = false;
		this.setLightLevel(lighted ? 15 : 0);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
	}
    
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state){
        super.breakBlock(world, pos, state);
        world.removeTileEntity(pos);
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess access, BlockPos pos){
    	if(!this.isOriented){
    		if(this.regularAABB == null){
    			TileEntityDecor tile = ((TileEntityDecor) access.getTileEntity(pos));
    			if(tile != null){
    				PackDecorObject pack = PackParserSystem.getDecor(tile.decorName);
    				if(pack != null){
    		    		this.regularAABB = new AxisAlignedBB(0.5F - pack.general.width/2F, 0, 0.5F - pack.general.depth/2F, 0.5F + pack.general.width/2F, pack.general.height, 0.5F +  pack.general.depth/2F);
    				}
    			}
    		}else{
    			return this.regularAABB;
    		}
    	}else{
    		if(this.regularAABB == null){
    			TileEntityDecor tile = ((TileEntityDecor) access.getTileEntity(pos));
    			if(tile != null){
    				PackDecorObject pack = PackParserSystem.getDecor(tile.decorName);
    				if(pack != null){
    					this.regularAABB = new AxisAlignedBB(0.5F - pack.general.width/2F, 0, 0.5F - pack.general.depth/2F, 0.5F + pack.general.width/2F, pack.general.height, 0.5F +  pack.general.depth/2F);
    					this.rotatedAABB = !this.isOriented ? regularAABB : new AxisAlignedBB(0.5F - pack.general.depth/2F, 0, 0.5F - pack.general.width/2F, 0.5F + pack.general.depth/2F, pack.general.height, 0.5F +  pack.general.width/2F);
    				}
    			}
    		}else{
    			TileEntityDecor tile = ((TileEntityDecor) access.getTileEntity(pos));
    			if(tile != null){
	    			if(tile.rotation%2 == 0){
	    				return regularAABB;
	    	        }else{
	    	        	return rotatedAABB;
	    	        }
    			}
    		}
    	}
        return super.getBoundingBox(state, access, pos);
    }
    
	@Override
	@SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity){
		if(!this.isOriented){
			super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity);
		}else{
			if(this.regularAABB == null){
    			TileEntityDecor tile = ((TileEntityDecor) world.getTileEntity(pos));
    			if(tile != null){
    				PackDecorObject pack = PackParserSystem.getDecor(tile.decorName);
    				if(pack != null){
    					this.regularAABB = new AxisAlignedBB(0.5F - pack.general.width/2F, 0, 0.5F - pack.general.depth/2F, 0.5F + pack.general.width/2F, pack.general.height, 0.5F +  pack.general.depth/2F);
    					this.rotatedAABB = !this.isOriented ? regularAABB : new AxisAlignedBB(0.5F - pack.general.depth/2F, 0, 0.5F - pack.general.width/2F, 0.5F + pack.general.depth/2F, pack.general.height, 0.5F +  pack.general.width/2F);
    				}
    			}
    			super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity);
    		}else{
    			TileEntityDecor tile = ((TileEntityDecor) world.getTileEntity(pos));
    			if(tile != null){
	    			if(tile.rotation%2 == 0){
	    				addCollisionBoxToList(pos, entityBox, collidingBoxes, regularAABB);
	    	        }else{
	    	        	addCollisionBoxToList(pos, entityBox, collidingBoxes, rotatedAABB);
	    	        }
    			}
    		}
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
