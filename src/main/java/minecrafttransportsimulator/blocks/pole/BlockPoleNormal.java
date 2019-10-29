package minecrafttransportsimulator.blocks.pole;

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

public class BlockPoleNormal extends Block{
	public static final PropertyBool UP = PropertyBool.create("up");
	public static final PropertyBool DOWN = PropertyBool.create("down");
	public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
	
    private final AxisAlignedBB CENTER_AABB;
    private final AxisAlignedBB UP_AABB;
    private final AxisAlignedBB DOWN_AABB;
    private final AxisAlignedBB NORTH_AABB;
    private final AxisAlignedBB EAST_AABB;
    private final AxisAlignedBB SOUTH_AABB;
    private final AxisAlignedBB WEST_AABB;
    
	public BlockPoleNormal(float poleRadius){
		super(Material.ROCK);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
		this.setCreativeTab(MTSRegistry.coreTab);
		this.setDefaultState(this.blockState.getBaseState().
				withProperty(UP, false).
				withProperty(DOWN, false).
				withProperty(NORTH, false).
				withProperty(EAST, false).
				withProperty(SOUTH, false).
				withProperty(WEST, false));
		CENTER_AABB = new AxisAlignedBB(0.5F - poleRadius, 0.5F - poleRadius, 0.5F - poleRadius, 0.5F + poleRadius, 0.5F + poleRadius, 0.5F + poleRadius);
		UP_AABB = new AxisAlignedBB(0.5F - poleRadius, 0.5F + poleRadius, 0.5F - poleRadius, 0.5F + poleRadius, 1.0, 0.5F + poleRadius);
		DOWN_AABB = new AxisAlignedBB(0.5F - poleRadius, 0.0F, 0.5F - poleRadius, 0.5F + poleRadius, 0.5F - poleRadius, 0.5F + poleRadius);
		NORTH_AABB = new AxisAlignedBB(0.5F - poleRadius, 0.5F - poleRadius, 0, 0.5F + poleRadius, 0.5F + poleRadius, 0.5F - poleRadius);
		EAST_AABB = new AxisAlignedBB(0.5F + poleRadius, 0.5F - poleRadius, 0.5F - poleRadius, 1.0, 0.5F + poleRadius, 0.5F + poleRadius);
		SOUTH_AABB = new AxisAlignedBB(0.5F - poleRadius, 0.5F - poleRadius, 0.5F + poleRadius, 0.5F + poleRadius, 0.5F + poleRadius, 1.0);
		WEST_AABB = new AxisAlignedBB(0.0, 0.5F - poleRadius, 0.5F - poleRadius, 0.5F - poleRadius, 0.5F + poleRadius, 0.5F + poleRadius);
	}
	
	@Override
	@SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean p_185477_7_){
        state = state.getActualState(world, pos);
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
	public int getMetaFromState(IBlockState state){
        return 0;
    }
	
	@Override
	@SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess access, BlockPos pos){
		for(EnumFacing facing : EnumFacing.VALUES){
			state = this.setStatesFor(state, access, pos, facing);
		}
		return state;
    }
	
    public boolean canConnectOnSide(IBlockAccess access, BlockPos pos, EnumFacing side){
		return true;
    }
	
	protected IBlockState setStatesFor(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing facing){
        IBlockState offsetState = access.getBlockState(pos.offset(facing));
        Block block = offsetState.getBlock();
        boolean connected = block instanceof BlockPoleNormal ? ((BlockPoleNormal) block).canConnectOnSide(access, pos.offset(facing), facing.getOpposite()) && this.canConnectOnSide(access, pos, facing) : false;
        
		switch (facing){
			case UP: return state.withProperty(UP, connected);
			case DOWN: return state.withProperty(DOWN, connected);
			case NORTH: return state.withProperty(NORTH, connected);
			case EAST: return state.withProperty(EAST, connected);
			case SOUTH: return state.withProperty(SOUTH, connected);
			case WEST: return state.withProperty(WEST, connected);
			default: return state;
		}
	}
    
    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, new IProperty[] {UP, DOWN, NORTH, EAST, SOUTH, WEST});
    }
}
