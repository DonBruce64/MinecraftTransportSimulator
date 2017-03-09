package minecraftflightsimulator.blocks;

import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockTrack extends BlockContainer{
	
	public BlockTrack(){
		super(Material.iron);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.25F, 1.0F);
	}
	
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta){
		TileEntityTrack track = (TileEntityTrack) BlockHelper.getTileEntityFromCoords(world, x, y, z);
		if(track != null){
			if(track.curve != null){
				int otherX = track.curve.blockEndPoint[0];
				int otherY = track.curve.blockEndPoint[1];
				int otherZ = track.curve.blockEndPoint[2];
				super.breakBlock(world, x, y, z, block, meta);
				BlockHelper.setBlockToAir(world, otherX, otherY, otherZ);
			}
			if(!track.isInvalid()){
				track.removeDummyTracks();
			}
		}
	}
	
	@Override
	public TileEntity createNewTileEntity(World world, int metadata){
		return new TileEntityTrack();
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
