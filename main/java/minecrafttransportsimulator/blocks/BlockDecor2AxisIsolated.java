package minecrafttransportsimulator.blocks;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockDecor2AxisIsolated extends ABlockDecor{
	public static final PropertyBool ROTATED = PropertyBool.create("rotated");
	
	private final AxisAlignedBB regularAABB;
	private final AxisAlignedBB rotatedAABB;

	public BlockDecor2AxisIsolated(Material material, float hardness, float resistance, float width, float height, float depth){
		super(material, hardness, resistance);
		this.setDefaultState(this.blockState.getBaseState().withProperty(ROTATED, false));
		this.regularAABB = new AxisAlignedBB(0.5F - width/2F, 0, 0.5F - depth/2F, 0.5F + width/2F, height, 0.5F +  depth/2F);
		this.rotatedAABB = new AxisAlignedBB(0.5F - depth/2F, 0, 0.5F - width/2F, 0.5F + depth/2F, height, 0.5F +  width/2F);
	}
	
    @Override
    public boolean canConnectOnSide(IBlockAccess access, BlockPos pos, EnumFacing side){
    	return false;
    }
	
	@Override
	@SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn){
        state = state.getActualState(worldIn, pos);
        if(state.getValue(ROTATED).booleanValue()){
            addCollisionBoxToList(pos, entityBox, collidingBoxes, rotatedAABB);
        }else{
        	addCollisionBoxToList(pos, entityBox, collidingBoxes, regularAABB);
        }
    }
	
	@Override
	public int getMetaFromState(IBlockState state){
        return 0;
    }
	
	@Override
	@SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess access, BlockPos pos){
        return state.withProperty(ROTATED, this.isRotated(access, pos));
    }
	
	private boolean isRotated(IBlockAccess worldIn, BlockPos pos){
        IBlockState blockstate = worldIn.getBlockState(pos.east());
        Block block = blockstate.getBlock();
        if(block.getRegistryName().equals(this.getRegistryName())){
        	return true;
        }
        
        blockstate = worldIn.getBlockState(pos.west());
        block = blockstate.getBlock();
        if(block.getRegistryName().equals(this.getRegistryName())){
        	return true;
        }
        
        return false;
	}
    
    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, new IProperty[] {ROTATED});
    }
}
