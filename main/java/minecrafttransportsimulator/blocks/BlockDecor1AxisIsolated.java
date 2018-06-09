package minecrafttransportsimulator.blocks;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockDecor1AxisIsolated extends ABlockDecor{
	private final AxisAlignedBB blockBox;

	public BlockDecor1AxisIsolated(Material material, float hardness, float resistance, float width, float height){
		super(material, hardness, resistance);
		this.blockBox = new AxisAlignedBB(0.5F - width/2F, 0, 0.5F - width/2F, 0.5F + width/2F, height, 0.5F +  width/2F);
	}
	
    @Override
    public boolean canConnectOnSide(IBlockAccess access, BlockPos pos, EnumFacing side){
    	return false;
    }
	
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos){
        return blockBox;
    }
}
