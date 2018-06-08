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

public class BlockDecor6AxisIsolated extends ABlockDecor{
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
    
	public BlockDecor6AxisIsolated(Material material, float hardness, float resistance, float diameter){
		super(material, hardness, resistance);
		this.setDefaultState(this.blockState.getBaseState().
				withProperty(UP, false).
				withProperty(DOWN, false).
				withProperty(NORTH, false).
				withProperty(EAST, false).
				withProperty(SOUTH, false).
				withProperty(WEST, false));
		float radius = diameter/2F;
		CENTER_AABB = new AxisAlignedBB(0.5F - radius, 0.5F - radius, 0.5F - radius, 0.5F + radius, 0.5F + radius, 0.5F + radius);
		UP_AABB = new AxisAlignedBB(0.5F - radius, 0.5F + radius, 0.5F - radius, 0.5F + radius, 1.0, 0.5F + radius);
		DOWN_AABB = new AxisAlignedBB(0.5F - radius, 0.0F, 0.5F - radius, 0.5F + radius, 0.5F - radius, 0.5F + radius);
		NORTH_AABB = new AxisAlignedBB(0.5F - radius, 0.5F - radius, 0, 0.5F + radius, 0.5F + radius, 0.5F - radius);
		EAST_AABB = new AxisAlignedBB(0.5F + radius, 0.5F - radius, 0.5F - radius, 1.0, 0.5F + radius, 0.5F + radius);
		SOUTH_AABB = new AxisAlignedBB(0.5F - radius, 0.5F - radius, 0.5F + radius, 0.5F + radius, 0.5F + radius, 1.0);
		WEST_AABB = new AxisAlignedBB(0.0, 0.5F - radius, 0.5F - radius, 0.5F - radius, 0.5F + radius, 0.5F + radius);
	}
	
	@Override
	@SuppressWarnings("deprecation")
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
	@SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos){
		for(EnumFacing facing : EnumFacing.VALUES){
			state = this.setStatesFor(state, worldIn, pos, facing);
		}
		return state;
    }
	
	protected IBlockState setStatesFor(IBlockState state, IBlockAccess worldIn, BlockPos pos, EnumFacing facing){
        IBlockState offsetState = worldIn.getBlockState(pos.offset(facing));
        Block block = offsetState.getBlock();
		switch (facing){
			case UP: return state.withProperty(UP, block instanceof BlockDecor6AxisIsolated);
			case DOWN: return state.withProperty(DOWN, block instanceof BlockDecor6AxisIsolated);
			case NORTH: return state.withProperty(NORTH, block instanceof BlockDecor6AxisIsolated);
			case EAST: return state.withProperty(EAST, block instanceof BlockDecor6AxisIsolated);
			case SOUTH: return state.withProperty(SOUTH, block instanceof BlockDecor6AxisIsolated);
			case WEST: return state.withProperty(WEST, block instanceof BlockDecor6AxisIsolated);
			default: return state;
		}
	}
    
    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, new IProperty[] {UP, DOWN, NORTH, EAST, SOUTH, WEST});
    }
}
