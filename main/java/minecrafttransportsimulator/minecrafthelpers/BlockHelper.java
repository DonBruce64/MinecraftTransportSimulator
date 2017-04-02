package minecrafttransportsimulator.minecrafthelpers;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class BlockHelper {
	public static Block getBlockFromCoords(World world, int x, int y, int z){
		return world.getBlock(x, y, z);
	}
	
	public static float getBlockHardness(World world, int x, int y, int z){
		return getBlockFromCoords(world, x, y, z).getBlockHardness(world, x, y, z);
	}
	
	public static boolean isBlockLiquid(Block block){
		return block.getMaterial().isLiquid();
	}
	
	public static boolean canPlaceBlockAt(World world, int x, int y, int z){
		return world.getBlock(x, y, z).canPlaceBlockAt(world, x, y, z);
	}
	
	public static void setBlockToAir(World world, int x, int y, int z){
		world.setBlockToAir(x, y, z);
	}
	
	public static TileEntity getTileEntityFromCoords(World world, int x, int y, int z){
		return world.getTileEntity(x, y, z);
	}
	
	public static boolean isPositionInLiquid(World world, double x, double y, double z){
		return isBlockLiquid(getBlockFromCoords(world, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
	}
	
	public static int getBlockLight(World world, int x, int y, int z){
		//DEL180START
		return world.getBlockLightValue_do(x, y, z, false);
		//DEL180END
		/*INS180
		return world.getLight(new BlockPos(x, y, z), false); 
		INS180*/
	}
	
	public static float getRenderLight(World world, int x, int y, int z){
		return world.getLightBrightnessForSkyBlocks(x, y, z, 0);
	}
}
