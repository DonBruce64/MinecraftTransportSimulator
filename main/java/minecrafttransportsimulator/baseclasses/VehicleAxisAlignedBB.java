package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**This class is an upgraded AABB class that allows for AABBs to put into a collective for
 * vehicle collision operations.  They contain rel and pos vectors to allow for collision
 * handling in simpler ways in the vehicle systems where they need to be rotated and
 * offset to deal with vehicle movement.  Note that pos is the actual center position of
 * this box, while rel is the relative offset center to the vehicle (identical to part offsets
 * positions when used for those).
 * 
 * @author don_bruce
 */
public class VehicleAxisAlignedBB extends AxisAlignedBB{
	public final boolean isInterior;
	public final boolean collidesWithLiquids;
	public final Vec3d pos;
	public final Vec3d rel;
	public final float width;
	public final float height;
	
	public VehicleAxisAlignedBB(Vec3d pos, Vec3d rel, float width, float height, boolean isInterior, boolean collidesWithLiquids){
		super(pos.x - width/2F, pos.y - height/2F, pos.z - width/2F, pos.x + width/2F, pos.y + height/2F, pos.z + width/2F);
		this.pos = pos;
		this.rel = rel;
		this.width = width;
		this.height = height;
		this.isInterior = isInterior;
		this.collidesWithLiquids = collidesWithLiquids;
	}
	
	public VehicleAxisAlignedBB getBoxWithOrigin(Vec3d pos){
		return new VehicleAxisAlignedBB(pos, this.rel, this.width, this.height, this.isInterior, this.collidesWithLiquids);
	}
	
	@Override
	public VehicleAxisAlignedBB offset(double xOffset, double yOffset, double zOffset){
		return getBoxWithOrigin(this.pos.addVector(xOffset, yOffset, zOffset));
	}
	
	@Override
	public VehicleAxisAlignedBB grow(double value){
		return new VehicleAxisAlignedBB(this.pos, this.rel, (float) (this.width + value*2F), (float) (this.height + value*2F), this.isInterior, this.collidesWithLiquids);
    }
	
	/**Handy collision check with extra features the default one doesn't have.*/
	public List<AxisAlignedBB> getAABBCollisions(World world, List<BlockPos> collidedBlockPos){
		int minTestX = (int) Math.floor(minX);
    	int maxTestX = (int) Math.floor(maxX + 1.0D);
    	int minTestY = (int) Math.floor(minY);
    	int maxTestY = (int) Math.floor(maxY + 1.0D);
    	int minTestZ = (int) Math.floor(minZ);
    	int maxTestZ = (int) Math.floor(maxZ + 1.0D);
    	List<AxisAlignedBB> collidingAABBList = new ArrayList<AxisAlignedBB>();
    	
    	for(int i = minTestX; i < maxTestX; ++i){
    		for(int j = minTestY; j < maxTestY; ++j){
    			for(int k = minTestZ; k < maxTestZ; ++k){
    				BlockPos pos = new BlockPos(i, j, k);
    				IBlockState state = world.getBlockState(pos);
    				if(state.getBlock().canCollideCheck(state, false)){
    					state.addCollisionBoxToList(world, pos, this, collidingAABBList, null, false);
        				if(collidedBlockPos != null){
        					collidedBlockPos.add(pos);
        				}
    				}
    			}
    		}
    	}
		return collidingAABBList;
	}
}
