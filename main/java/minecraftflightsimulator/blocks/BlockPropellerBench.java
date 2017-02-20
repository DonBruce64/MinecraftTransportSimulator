package minecraftflightsimulator.blocks;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockPropellerBench extends BlockContainer{	

	public BlockPropellerBench(){
		super(Material.iron);
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ){
		if(player.getDistance(x, y, z) < 5){
			if(!world.isRemote && player.isSneaking()){
				((TileEntityPropellerBench) BlockHelper.getTileEntityFromCoords(world, x, y, z)).dropPropellerAt(player.posX, player.posY, player.posZ);
			}else if(world.isRemote && !player.isSneaking()){
				MFS.proxy.openGUI(BlockHelper.getTileEntityFromCoords(world, x, y, z), player);
			}
		}
		return true;
	}
	
	@Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta){
		if(!world.isRemote){
			((TileEntityPropellerBench) BlockHelper.getTileEntityFromCoords(world, x, y, z)).dropPropellerAt(x, y, z);
		}
		super.breakBlock(world, x, y, z, block, meta);
    }
	
	@Override
	public TileEntity createNewTileEntity(World world, int metadata){
		return new TileEntityPropellerBench();
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
