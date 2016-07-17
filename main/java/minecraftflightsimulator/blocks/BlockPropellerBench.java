package minecraftflightsimulator.blocks;

import minecraftflightsimulator.MFS;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockPropellerBench extends BlockContainer{	

	public BlockPropellerBench(){
		super(Material.iron);
		this.setBlockName("PropellerBench");
		this.setCreativeTab(MFS.tabMFS);
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ){
		if(!world.isRemote && ((TileEntityPropellerBench) world.getTileEntity(x, y, z)).isUseableByPlayer(player)){
			player.openGui(MFS.instance, -1, world, x, y, z);
		}
		return true;
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
