package minecrafttransportsimulator.wrappers;

import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockAxial;
import minecrafttransportsimulator.blocks.components.ABlockAxial.Axis;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**Wrapper for the MC Block class that has not only rotation but axis information.
 * This axis information consists of 6 boolean states for up, down, north,
 * south, east, and west.  These will be set in the direction of similar axial
 * blocks, or solid blocks if {@link ABlockAxial#canConnectToSolids} returns true.
 *
 * @author don_bruce
 */
public class WrapperBlockAxial extends WrapperBlock{
	public static final PropertyBool UP = PropertyBool.create("up");
	public static final PropertyBool DOWN = PropertyBool.create("down");
	public static final PropertyBool NORTH = PropertyBool.create("north");
	public static final PropertyBool SOUTH = PropertyBool.create("south");
	public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool WEST = PropertyBool.create("west");
    
    public static final PropertyBool UP_SOLID = PropertyBool.create("up_solid");
	public static final PropertyBool DOWN_SOLID = PropertyBool.create("down_solid");
	public static final PropertyBool NORTH_SOLID = PropertyBool.create("north_solid");
	public static final PropertyBool SOUTH_SOLID = PropertyBool.create("south_solid");
	public static final PropertyBool EAST_SOLID = PropertyBool.create("east_solid");
    public static final PropertyBool WEST_SOLID = PropertyBool.create("west_solid");
	
    public WrapperBlockAxial(ABlockBase block){
		super(block);
		//Need to add-on extra properties to this block, otherwise MC gets angry.
		setDefaultState(this.getDefaultState().
				withProperty(UP, false).
				withProperty(DOWN, false).
				withProperty(NORTH, false).
				withProperty(SOUTH, false).
				withProperty(EAST, false).
				withProperty(WEST, false).
				withProperty(UP_SOLID, false).
				withProperty(DOWN_SOLID, false).
				withProperty(NORTH_SOLID, false).
				withProperty(SOUTH_SOLID, false).
				withProperty(EAST_SOLID, false).
				withProperty(WEST_SOLID, false));
	}
    
    /**
	 *  Updates the state of axis connections.
	 *  Block-insensitive, but assumes block is instance of this wrapper.
	 */
    public static void updateAxisStates(WrapperWorld world, Point3i point, Map<Axis, Boolean> states){
    	BlockPos pos = new BlockPos(point.x, point.y, point.z);
    	IBlockState state = world.world.getBlockState(pos);
    	//Add-on actual dyanimc state based on neighbors.
    	for(EnumFacing facing : EnumFacing.VALUES){
			state = setStatesFor(state, world.world, pos, facing);
		}
    	for(Axis axis : Axis.values()){
    		switch (axis){
				case UP: states.put(axis, state.getValue(UP).booleanValue()); break;
				case DOWN: states.put(axis, state.getValue(DOWN).booleanValue()); break;
				case NORTH: states.put(axis, state.getValue(NORTH).booleanValue()); break;
				case SOUTH: states.put(axis, state.getValue(SOUTH).booleanValue()); break;
				case EAST: states.put(axis, state.getValue(EAST).booleanValue()); break;
				case WEST: states.put(axis, state.getValue(WEST).booleanValue()); break;
				default: states.put(axis, true);
			}
    	}
    }
    
    /**
	 *  Helper method to set the boolean states.
	 */
	protected static IBlockState setStatesFor(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing facing){
		//Get block info.
		IBlockState offsetMCState = access.getBlockState(pos.offset(facing));
		Block offsetMCBlock = offsetMCState.getBlock();
		ABlockAxial offsetAxialBlock = offsetMCBlock instanceof WrapperBlockAxial ? (ABlockAxial) ((WrapperBlockAxial) offsetMCBlock).block : null;
		
		//Get block state flags.
        boolean similarBlockOnSide = offsetAxialBlock != null ? offsetAxialBlock.canConnectOnFront() || !access.getBlockState(pos.offset(facing)).getValue(FACING).equals(facing.getOpposite()) : false;
        boolean solidOnSide = offsetMCBlock != null ? ((ABlockAxial) ((WrapperBlockAxial) access.getBlockState(pos).getBlock()).block).canConnectToSolids() && !offsetMCBlock.equals(Blocks.BARRIER) && offsetMCState.getMaterial().isOpaque() && offsetMCState.isFullCube() && offsetMCState.getMaterial() != Material.GOURD : false;
        
        //Set the state.
		switch (facing){
			case UP: return state.withProperty(UP, similarBlockOnSide).withProperty(UP_SOLID, solidOnSide);
			case DOWN: return state.withProperty(DOWN, similarBlockOnSide).withProperty(DOWN_SOLID, solidOnSide);
			case NORTH: return state.withProperty(NORTH, similarBlockOnSide).withProperty(NORTH_SOLID, solidOnSide);
			case SOUTH: return state.withProperty(SOUTH, similarBlockOnSide).withProperty(SOUTH_SOLID, solidOnSide);
			case EAST: return state.withProperty(EAST, similarBlockOnSide).withProperty(EAST_SOLID, solidOnSide);
			case WEST: return state.withProperty(WEST, similarBlockOnSide).withProperty(WEST_SOLID, solidOnSide);
			default: return state;
		}
	}
	
    @Override
    protected BlockStateContainer createBlockState(){
    	//Need to have both our states and the one from the main wrapper.
    	return new BlockStateContainer(this, new IProperty[] {FACING, UP, UP_SOLID, DOWN, DOWN_SOLID, NORTH, NORTH_SOLID, EAST, EAST_SOLID, SOUTH, SOUTH_SOLID, WEST, WEST_SOLID});
    }
    
    @Override
	@SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess access, BlockPos pos){
    	//Need to check here so the JSON files get the right states.
		for(EnumFacing facing : EnumFacing.VALUES){
			state = setStatesFor(state, access, pos, facing);
		}
		return state;
    }
}
