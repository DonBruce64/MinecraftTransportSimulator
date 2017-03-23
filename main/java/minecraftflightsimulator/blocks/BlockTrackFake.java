package minecraftflightsimulator.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class BlockTrackFake extends Block{
	public static boolean overrideBreakingBlocks = false;
	private static List<int[]> blockCheckCoords = new ArrayList<int[]>();

	public BlockTrackFake(){
		super(Material.iron);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
	}
	
	@Override
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB box, List list, Entity entity){
		byte metadata = BlockHelper.getBlockMetadata(world, x, y, z);
		this.setBlockBounds(0, 0, 0, 1, metadata/16F, 1);
		super.addCollisionBoxesToList(world, x, y, z, box, list, entity);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z){
		byte metadata = BlockHelper.getBlockMetadata(world, x, y, z);
		this.setBlockBounds(0, 0, 0, 1, metadata/16F, 1);
		return super.getSelectedBoundingBoxFromPool(world, x, y, z);
	}
	
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int metadata){
		if(!overrideBreakingBlocks){
			//Add current block to list.
			blockCheckCoords.add(new int[]{x, y, z});
			for(byte i=-1; i<=1; ++i){
				for(byte j=-1; j<=1; ++j){
					for(byte k=-1; k<=1; ++k){
						//Make sure we aren't checking the current block.
						if(!(i==0 && j==0 && k==0)){
							//Make sure the block hasn't been already checked.
							for(int[] coords : blockCheckCoords){
								if(x + i == coords[0] && y + j == coords[1] && z + k == coords[2]){
									//Block already checked.
									return;
								}
							}
							if(BlockHelper.getTileEntityFromCoords(world, x + i, y + j, z + k) instanceof TileEntityTrack){
								//Found a track TE.  See if it's the parent for this fake track block.
								for(int[] track : ((TileEntityTrack) BlockHelper.getTileEntityFromCoords(world, x + i, y + j, z + k)).getFakeTracks()){
									if(track[0] == x && track[1] == y && track[2] == z){
										//Track TE contains this fake track.  Set master track block to air and hand off breaking.
										overrideBreakingBlocks = true;
										BlockHelper.setBlockToAir(world, x + i, y + j, z + k);
										blockCheckCoords.clear();
										return;
									}
								}
							}else if(BlockHelper.getBlockFromCoords(world, x, y, z) instanceof BlockTrackFake){
								//Found another fake track.  Call it's break code to pass on the message.
								((BlockTrackFake) BlockHelper.getBlockFromCoords(world, x, y, z)).breakBlock(world, x, y, z, block, metadata);
							}
						}
					}
				}
			}
		}
		super.breakBlock(world, x, y, z, block, metadata);
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
