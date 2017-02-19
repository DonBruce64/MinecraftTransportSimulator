package minecraftflightsimulator.minecrafthelpers;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public final class AABBHelper{
	private static int minX;
	private static int maxX;
	private static int minY;
	private static int maxY;
	private static int minZ;
	private static int maxZ;
	
	public static AxisAlignedBB getBlockBoundingBox(World world, int x, int y, int z){
		//DEL180START
		return BlockHelper.getBlockFromCoords(world, x, y, z).getCollisionBoundingBoxFromPool(world, x, y, z);
		//DEL180END
		/*INS180
		BlockPos pos = new BlockPos(i, j, k);
        IBlockState state = worldObj.getBlockState(pos);
        return = state.getBlock().getCollisionBoundingBox(worldObj, pos, state);
		INS180*/
	}
	
	public static AxisAlignedBB getEntityBoundingBox(Entity entity){
		return entity.getBoundingBox();
	}
	
	public static AxisAlignedBB getOffsetEntityBoundingBox(Entity entity, float offsetX, float offsetY, float offsetZ){
		return getEntityBoundingBox(entity).copy().offset(offsetX, offsetY, offsetZ);
	}
	
	public static Map<AxisAlignedBB, Integer[]> getCollidingBlockBoxes(World world, AxisAlignedBB box, boolean countLiquids){
		Map<AxisAlignedBB, Integer[]> collidingBoxes = new HashMap<AxisAlignedBB, Integer[]>();
		minX = (int) Math.floor(box.minX);
		maxX = (int) Math.floor(box.maxX + 1.0D);
		minY = (int) Math.floor(box.minY);
		maxY = (int) Math.floor(box.maxY + 1.0D);
		minZ = (int) Math.floor(box.minZ);
		maxZ = (int) Math.floor(box.maxZ + 1.0D);
		for(int i = minX; i < maxX; ++i){
        	for(int j = minY; j < maxY; ++j){
        		for(int k = minZ; k < maxZ; ++k){
        			AxisAlignedBB blockBox = getBlockBoundingBox(world, i, j, k);
        			if(blockBox != null && box.intersectsWith(blockBox)){
        				collidingBoxes.put(blockBox, new Integer[]{i, j, k});
                    }else if(countLiquids){
                    	if(BlockHelper.isBlockLiquid(BlockHelper.getBlockFromCoords(world, i, j, k))){
                    		collidingBoxes.put(AxisAlignedBB.getBoundingBox(i, j, k, i+1, j+1, k+1), null);
                    	}
                    }
        		}
        	}
		}
		return ImmutableMap.copyOf(collidingBoxes);
	}
}