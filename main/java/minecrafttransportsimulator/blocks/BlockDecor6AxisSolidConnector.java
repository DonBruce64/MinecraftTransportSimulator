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

public class BlockDecor6AxisSolidConnector extends BlockDecor6AxisRegular{
	public static final PropertyBool UP_SOLID = PropertyBool.create("up_solid");
	public static final PropertyBool DOWN_SOLID = PropertyBool.create("down_solid");
	public static final PropertyBool NORTH_SOLID = PropertyBool.create("north_solid");
    public static final PropertyBool EAST_SOLID = PropertyBool.create("east_solid");
    public static final PropertyBool SOUTH_SOLID = PropertyBool.create("south_solid");
    public static final PropertyBool WEST_SOLID = PropertyBool.create("west_solid");
    
	public BlockDecor6AxisSolidConnector(Material material, float hardness, float resistance){
		super(material, hardness, resistance);
		this.setDefaultState(super.getDefaultState().
				withProperty(UP_SOLID, false).
				withProperty(DOWN_SOLID, false).
				withProperty(NORTH_SOLID, false).
				withProperty(EAST_SOLID, false).
				withProperty(SOUTH_SOLID, false).
				withProperty(WEST_SOLID, false));
	}
	
	@Override
	protected IBlockState setStatesFor(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing facing){
		state = super.setStatesFor(state, world, pos, facing);
        IBlockState offsetState = world.getBlockState(pos.offset(facing));
        Block block = offsetState.getBlock();
        boolean connectedToSolid = !block.equals(Blocks.BARRIER) && offsetState.getMaterial().isOpaque() && offsetState.isFullCube() && offsetState.getMaterial() != Material.GOURD;
    	
		switch (facing){
			case UP: return state.withProperty(UP_SOLID, connectedToSolid);
			case DOWN: return state.withProperty(DOWN_SOLID, connectedToSolid);
			case NORTH: return state.withProperty(NORTH_SOLID, connectedToSolid);
			case EAST: return state.withProperty(EAST_SOLID, connectedToSolid);
			case SOUTH: return state.withProperty(SOUTH_SOLID, connectedToSolid);
			case WEST: return state.withProperty(WEST_SOLID, connectedToSolid);
			default: return state;
		}
	}
    
    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, new IProperty[] {UP, UP_SOLID, DOWN, DOWN_SOLID, NORTH, NORTH_SOLID, EAST, EAST_SOLID, SOUTH, SOUTH_SOLID, WEST, WEST_SOLID});
    }
}
