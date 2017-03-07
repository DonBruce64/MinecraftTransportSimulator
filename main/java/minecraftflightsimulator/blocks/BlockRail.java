package minecraftflightsimulator.blocks;

import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockRail extends BlockContainer{
	
	public BlockRail(){
		super(Material.iron);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
	}
	
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta){
		TileEntityRail rail = (TileEntityRail) BlockHelper.getTileEntityFromCoords(world, x, y, z);
		if(rail != null){
			if(rail.curve != null){
				int otherX = (int) (rail.curve.endPoint[0] - 0.5F);
				int otherY = (int) rail.curve.endPoint[1];
				int otherZ = (int) (rail.curve.endPoint[2] - 0.5F);
				super.breakBlock(world, x, y, z, block, meta);
				BlockHelper.setBlockToAir(world, otherX, otherY, otherZ);
			}
		}
	}
	
	@Override
	public TileEntity createNewTileEntity(World world, int metadata){
		return new TileEntityRail();
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
