package minecraftflightsimulator.blocks;

import java.util.Random;

import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class BlockTrack extends BlockContainer{
	
	public BlockTrack(){
		super(Material.iron);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.25F, 1.0F);
	}
	
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int metadata){
		TileEntityTrack track = (TileEntityTrack) BlockHelper.getTileEntityFromCoords(world, x, y, z);
		if(track != null){
			if(track.curve != null){
				if(track.isPrimary && !world.isRemote){
					int numberTracks = (int) track.curve.pathLength;
					while(numberTracks > 0){
						int tracksInItem = Math.min(numberTracks, 64);
						world.spawnEntityInWorld(new EntityItem(world, x, y, z, new ItemStack(MFSRegistry.track, tracksInItem, metadata)));
						numberTracks -= tracksInItem;
					}
					track.removeDummyTracks();
				}
				int otherX = track.curve.blockEndPoint[0];
				int otherY = track.curve.blockEndPoint[1];
				int otherZ = track.curve.blockEndPoint[2];
				super.breakBlock(world, x, y, z, block, metadata);
				BlockHelper.setBlockToAir(world, otherX, otherY, otherZ);
			}
		}
	}
	
	@Override
    public Item getItemDropped(int metadata, Random rand, int fortune){
        return null;
    }
	
	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player){
        return null;
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
