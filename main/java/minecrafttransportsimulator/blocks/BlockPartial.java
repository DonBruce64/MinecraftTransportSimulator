package minecrafttransportsimulator.blocks;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
	
public class BlockPartial extends Block{
	private final AxisAlignedBB blockBox;
	
    public BlockPartial(Material material, float width, float height){
		super(material);
		this.fullBlock = false;
		this.setCreativeTab(MTSRegistry.coreTab);
		this.blockBox = new AxisAlignedBB(0.5F - width/2F, 0, 0.5F - width/2F, 0.5F + width/2F, height, 0.5F +  width/2F);
	}
    
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos){
        return blockBox;
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
}
