package minecraftflightsimulator.blocks;

import java.util.Random;

import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class BlockTrackFake extends BlockContainer{

	public BlockTrackFake(){
		super(Material.iron);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
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
	public TileEntity createNewTileEntity(World world, int metadata){
		return new TileEntityTrackFake();
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
