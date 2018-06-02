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
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockDecor6Axis extends ABlockDecor{
	public static final PropertyBool UP = PropertyBool.create("up");
	public static final PropertyBool UP_SOLID = PropertyBool.create("up_solid");
	public static final PropertyBool DOWN = PropertyBool.create("down");
	public static final PropertyBool DOWN_SOLID = PropertyBool.create("down_solid");
	public static final PropertyBool NORTH = PropertyBool.create("north");
	public static final PropertyBool NORTH_SOLID = PropertyBool.create("north_solid");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool EAST_SOLID = PropertyBool.create("east_solid");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool SOUTH_SOLID = PropertyBool.create("south_solid");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool WEST_SOLID = PropertyBool.create("west_solid");
	
    private final boolean connectsToSolids;
    private final AxisAlignedBB CENTER_AABB;
    private final AxisAlignedBB UP_AABB;
    private final AxisAlignedBB DOWN_AABB;
    private final AxisAlignedBB NORTH_AABB;
    private final AxisAlignedBB EAST_AABB;
    private final AxisAlignedBB SOUTH_AABB;
    private final AxisAlignedBB WEST_AABB;
    
	public BlockDecor6Axis(Material material, float hardness, float resistance, float diameter, boolean connectsToSolids){
		super(material, hardness, resistance);
		this.setDefaultState(this.blockState.getBaseState().withProperty(UP, false).withProperty(UP_SOLID, false).
				withProperty(DOWN, false).withProperty(DOWN_SOLID, false).
				withProperty(NORTH, false).withProperty(NORTH_SOLID, false).
				withProperty(EAST, false).withProperty(EAST_SOLID, false).
				withProperty(SOUTH, false).withProperty(SOUTH_SOLID, false).
				withProperty(WEST, false).withProperty(WEST_SOLID, false));
		this.connectsToSolids = connectsToSolids;
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
		for(EnumFacing facing : EnumFacing.VALUES){
			state = this.setStatesFor(state, worldIn, pos, facing);
		}
		return state;
    }
	
	private IBlockState setStatesFor(IBlockState state, IBlockAccess worldIn, BlockPos pos, EnumFacing facing){
        IBlockState offsetState = worldIn.getBlockState(pos.offset(facing));
        Block block = offsetState.getBlock();
        boolean connected = false;
        boolean connectedToSolid = false;
        
    	if(block instanceof BlockDecor6Axis){
    		connected = true;
    	}else if(connectsToSolids){
        	if(!block.equals(Blocks.BARRIER) && offsetState.getMaterial().isOpaque() && offsetState.isFullCube() && offsetState.getMaterial() != Material.GOURD){
        		connected = true;
        		connectedToSolid = true;
        	}
        }
    	
		switch (facing){
			case UP: return state.withProperty(UP, connected).withProperty(UP_SOLID, connectedToSolid);
			case DOWN: return state.withProperty(DOWN, connected).withProperty(DOWN_SOLID, connectedToSolid);
			case NORTH: return state.withProperty(NORTH, connected).withProperty(NORTH_SOLID, connectedToSolid);
			case EAST: return state.withProperty(EAST, connected).withProperty(EAST_SOLID, connectedToSolid);
			case SOUTH: return state.withProperty(SOUTH, connected).withProperty(SOUTH_SOLID, connectedToSolid);
			case WEST: return state.withProperty(WEST, connected).withProperty(WEST_SOLID, connectedToSolid);
			default: return state;
		}
	}
    
    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, new IProperty[] {UP, UP_SOLID, DOWN, DOWN_SOLID, NORTH, NORTH_SOLID, EAST, EAST_SOLID, SOUTH, SOUTH_SOLID, WEST, WEST_SOLID});
    }
}
