package minecraftflightsimulator.blocks;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.baseclasses.MTSBlockTileEntity;
import minecraftflightsimulator.baseclasses.MTSTileEntity;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import minecraftflightsimulator.minecrafthelpers.PlayerHelper;
import minecraftflightsimulator.packets.general.ChatPacket;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

public class BlockPropellerBench extends MTSBlockTileEntity{	

	public BlockPropellerBench(){
		super(Material.iron, 5.0F, 10.0F);
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ){
		if(player.getDistance(x, y, z) < 5){
			TileEntityPropellerBench bench = (TileEntityPropellerBench) BlockHelper.getTileEntityFromCoords(world, x, y, z);
			if(!world.isRemote){
				if(bench.getPropellerOnBench() != null){
					bench.dropPropellerAt(player.posX, player.posY, player.posZ);
				}else if(bench.isRunning()){
					MFS.MFSNet.sendTo(new ChatPacket(PlayerHelper.getTranslatedText("interact.failure.propellerbenchworking")), (EntityPlayerMP) player);
				}
			}else{
				if(!bench.isRunning() && bench.getPropellerOnBench() == null){
					MFS.proxy.openGUI(BlockHelper.getTileEntityFromCoords(world, x, y, z), player);
				}
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
	public MTSTileEntity getTileEntity(){
		return new TileEntityPropellerBench();
	}

	@Override
	protected boolean isBlock3D(){
		return true;
	}

	@Override
	protected void setDefaultBlockBounds(){
		this.setBlockBounds(0, 0, 0, 1, 1, 1);
	}

	@Override
	protected void setBlockBoundsFromMetadata(int metadata){
		setDefaultBlockBounds();
	}	
}
