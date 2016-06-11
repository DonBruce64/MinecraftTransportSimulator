package minecraftflightsimulator.blocks;

import minecraftflightsimulator.MFS;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class BlockPropellerBench extends Block{

	public BlockPropellerBench(){
		super(Material.rock);
		this.setBlockName("PropellerBench");
		this.setCreativeTab(MFS.tabMFS);
		this.textureName = "mfs:propellerbench";
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			player.openGui(MFS.instance, -1, world, x, y, z);
		}
		return true;
	}

}
