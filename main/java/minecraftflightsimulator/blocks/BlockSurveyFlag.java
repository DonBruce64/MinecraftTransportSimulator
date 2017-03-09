package minecraftflightsimulator.blocks;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockSurveyFlag extends BlockContainer{
	
	public BlockSurveyFlag(){
		super(Material.wood);
		this.setCreativeTab(MFS.tabMFS);
		this.setBlockBounds(0.4375F, 0.0F, 0.4375F, 0.5625F, 1F, 0.5625F);
	}
	
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta){
		((TileEntitySurveyFlag) BlockHelper.getTileEntityFromCoords(world, x, y, z)).clearFlagLinking();
		super.breakBlock(world, x, y, z, block, meta);
	}
	
	@Override
	public TileEntity createNewTileEntity(World world, int metadata){
		return new TileEntitySurveyFlag();
	}
	
	@Override
    public int getRenderType(){
        return -1;
    }
	
	@Override
    public boolean renderAsNormalBlock(){
        return false;
    }
	
	@Override
	public boolean isOpaqueCube(){
		return false;
	}
}
