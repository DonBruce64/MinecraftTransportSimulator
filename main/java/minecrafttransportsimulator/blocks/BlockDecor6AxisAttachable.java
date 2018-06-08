package minecrafttransportsimulator.blocks;

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

public class BlockDecor6AxisAttachable extends BlockDecor6AxisIsolated{
	public static final PropertyBool UP_SOLID = PropertyBool.create("up_solid");
	public static final PropertyBool DOWN_SOLID = PropertyBool.create("down_solid");
	public static final PropertyBool NORTH_SOLID = PropertyBool.create("north_solid");
    public static final PropertyBool EAST_SOLID = PropertyBool.create("east_solid");
    public static final PropertyBool SOUTH_SOLID = PropertyBool.create("south_solid");
    public static final PropertyBool WEST_SOLID = PropertyBool.create("west_solid");
    
	public BlockDecor6AxisAttachable(Material material, float hardness, float resistance, float diameter){
		super(material, hardness, resistance, diameter);
		this.setDefaultState(super.getDefaultState().
				withProperty(UP_SOLID, false).
				withProperty(DOWN_SOLID, false).
				withProperty(NORTH_SOLID, false).
				withProperty(EAST_SOLID, false).
				withProperty(SOUTH_SOLID, false).
				withProperty(WEST_SOLID, false));
	}
	
	@Override
	protected IBlockState setStatesFor(IBlockState state, IBlockAccess worldIn, BlockPos pos, EnumFacing facing){
        IBlockState offsetState = worldIn.getBlockState(pos.offset(facing));
        Block block = offsetState.getBlock();
        boolean connected = false;
        boolean connectedToSolid = false;
    	if(block instanceof BlockDecor6AxisIsolated){
    		connected = true;
    	}    	
        if(!block.equals(Blocks.BARRIER) && offsetState.getMaterial().isOpaque() && offsetState.isFullCube() && offsetState.getMaterial() != Material.GOURD){
        	connectedToSolid = true;
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
