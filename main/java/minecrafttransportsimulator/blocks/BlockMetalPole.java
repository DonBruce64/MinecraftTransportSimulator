package minecrafttransportsimulator.blocks;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
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

public class BlockMetalPole extends BlockPartial{
	public static final PropertyBool UP = PropertyBool.create("up");
	public static final PropertyBool DOWN = PropertyBool.create("down");
	public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
	
    public static final AxisAlignedBB CENTER_AABB = new AxisAlignedBB(0.375, 0.375, 0.375, 0.625, 0.625, 0.625);
    public static final AxisAlignedBB UP_AABB = new AxisAlignedBB(0.375, 0.625, 0.375, 0.625, 1.0, 0.625);
    public static final AxisAlignedBB DOWN_AABB = new AxisAlignedBB(0.375, 0.0, 0.375, 0.625, 0.375, 0.625);
    public static final AxisAlignedBB NORTH_AABB = new AxisAlignedBB(0.375, 0.375, 0.0, 0.625, 0.625, 0.375);
    public static final AxisAlignedBB EAST_AABB = new AxisAlignedBB(0.625, 0.375, 0.375, 1.0, 0.625, 0.625);
    public static final AxisAlignedBB SOUTH_AABB = new AxisAlignedBB(0.375, 0.375, 0.625, 0.625, 0.625, 1.0);
    public static final AxisAlignedBB WEST_AABB = new AxisAlignedBB(0.0, 0.375, 0.375, 0.375, 0.625, 0.625);
    
    
	private final boolean isBase;

	public BlockMetalPole(boolean isBase){
		super(Material.IRON, 1.0F, 1.0F);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
		this.setCreativeTab(MTSRegistry.coreTab);
		this.isBase = isBase;
		this.setDefaultState(this.blockState.getBaseState().withProperty(UP, true).withProperty(DOWN, true).withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false).withProperty(WEST, false));
	}
	
	@Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn){
        state = state.getActualState(worldIn, pos);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, CENTER_AABB);
        if(state.getValue(UP).booleanValue()){
            addCollisionBoxToList(pos, entityBox, collidingBoxes, UP_AABB);
        }
        if(state.getValue(DOWN).booleanValue()){
            addCollisionBoxToList(pos, entityBox, collidingBoxes, DOWN_AABB);
        }
        if(state.getValue(NORTH).booleanValue()){
            addCollisionBoxToList(pos, entityBox, collidingBoxes, NORTH_AABB);
        }
        if(state.getValue(EAST).booleanValue()){
            addCollisionBoxToList(pos, entityBox, collidingBoxes, EAST_AABB);
        }
        if(state.getValue(SOUTH).booleanValue()){
            addCollisionBoxToList(pos, entityBox, collidingBoxes, SOUTH_AABB);
        }
        if(state.getValue(WEST).booleanValue()){
            addCollisionBoxToList(pos, entityBox, collidingBoxes, WEST_AABB);
        }
    }
	
	@Override
	public int getMetaFromState(IBlockState state){
        return 0;
    }
	
	@Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos){
        return state.withProperty(UP, this.canConnectTo(worldIn, pos, EnumFacing.UP)).withProperty(DOWN, this.canConnectTo(worldIn, pos, EnumFacing.DOWN)).withProperty(NORTH, this.canConnectTo(worldIn, pos, EnumFacing.NORTH)).withProperty(EAST, this.canConnectTo(worldIn, pos, EnumFacing.EAST)).withProperty(SOUTH, this.canConnectTo(worldIn, pos, EnumFacing.SOUTH)).withProperty(WEST, this.canConnectTo(worldIn, pos, EnumFacing.WEST));
    }
	
	private boolean canConnectTo(IBlockAccess worldIn, BlockPos pos, EnumFacing facing){
        IBlockState iblockstate = worldIn.getBlockState(pos.offset(facing));
        Block block = iblockstate.getBlock();
        if(!isBase){
        	if(block instanceof BlockMetalPole){
        		if(!((BlockMetalPole) block).isBase){
        			return true;
        		}else{
        			return facing.equals(EnumFacing.DOWN);
        		}
        	}
        }
        return false;
	}
    
    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, new IProperty[] {UP, DOWN, NORTH, EAST, SOUTH, WEST});
    }
}
