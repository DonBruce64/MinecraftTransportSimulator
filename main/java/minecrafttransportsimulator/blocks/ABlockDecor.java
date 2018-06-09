package minecrafttransportsimulator.blocks;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
	
public abstract class ABlockDecor extends Block{
	
    public ABlockDecor(Material material, float hardness, float resistance){
		super(material);
		this.fullBlock = false;
		this.setHardness(hardness);
		this.setResistance(resistance);
		this.setCreativeTab(MTSRegistry.coreTab);
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
    
    protected abstract boolean canConnectOnSide(IBlockAccess access, BlockPos pos, EnumFacing side);
}
