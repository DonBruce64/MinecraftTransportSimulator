package minecrafttransportsimulator.blocks;

import minecrafttransportsimulator.baseclasses.MTSBlockTileEntity;
import minecrafttransportsimulator.baseclasses.MTSTileEntity;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class BlockTrack extends MTSBlockTileEntity{
	
	public BlockTrack(){
		super(Material.iron, 5.0F, 10.0F);
	}
	
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int metadata){
		TileEntityTrack track = (TileEntityTrack) BlockHelper.getTileEntityFromCoords(world, x, y, z);
		if(track != null){
			if(track.curve != null){
				if(!world.isRemote){
					int numberTracks = (int) track.curve.pathLength;
					while(numberTracks > 0){
						int tracksInItem = Math.min(numberTracks, 64);
						world.spawnEntityInWorld(new EntityItem(world, x, y, z, new ItemStack(MTSRegistry.track, tracksInItem, metadata)));
						numberTracks -= tracksInItem;
					}
					track.removeFakeTracks();
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
	public MTSTileEntity getTileEntity(){
		return new TileEntityTrack();
	}

	@Override
	protected boolean isBlock3D(){
		return true;
	}

	@Override
	protected void setDefaultBlockBounds(){
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.25F, 1.0F);
	}

	@Override
	protected void setBlockBoundsFromMetadata(int metadata){
		this.setDefaultBlockBounds();
	}
}
