package minecraftflightsimulator.blocks;

import java.util.List;
import java.util.Random;

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

	public BlockTrackFake(){
		super(Material.iron);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
	}
	
	@Override
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB box, List list, Entity entity){
		TileEntityTrackFake tile = (TileEntityTrackFake) BlockHelper.getTileEntityFromCoords(world, x, y, z);
		System.out.println("BOUNDS");
		//TODO not called unless block is highlighted.
		this.setBlockBounds(0, 0, 0, 1, tile.height, 1);
		super.addCollisionBoxesToList(world, x, y, z, box, list, entity);
	}
	
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int metadata){
		TileEntityTrackFake fake = (TileEntityTrackFake) BlockHelper.getTileEntityFromCoords(world, x, y, z);
		BlockHelper.setBlockToAir(world, fake.masterTrackPos[0], fake.masterTrackPos[1], fake.masterTrackPos[2]);
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
	public boolean isOpaqueCube(){
		return false;
	}
}
