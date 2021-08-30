package minecrafttransportsimulator.blocks.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Slightly-less basic block class.  This class is used for collision operations where a non-standard collision is required.
 * Mainly used on roads, but could be used on other things in the future.
 *
 * @author don_bruce
 */
public class BlockCollision extends ABlockBase{
	public static List<BlockCollision> blockInstances = createCollisionBlocks();
	public final BoundingBox blockBounds;
	
    public BlockCollision(int collisionHeightInPixels){
    	super(10.0F, 5.0F);
    	if(collisionHeightInPixels == 0){
    		collisionHeightInPixels = 1;
    	}
    	float heightRadiusRequired = collisionHeightInPixels/16F/2F;
    	float centerPositionOffset = -(0.5F - heightRadiusRequired);
    	this.blockBounds = new BoundingBox(new Point3d(0, centerPositionOffset, 0), 0.5D, heightRadiusRequired, 0.5D);
	}
    
    @Override
    public void onBroken(WrapperWorld world, Point3d position){
    	TileEntityRoad masterBlock = getMasterRoad(world, position);
    	if(masterBlock != null && masterBlock.isActive()){
    		//We belong to this TE.  Destroy the block.  This will end up
			//destroying all collisions, including this one.  However, since
			//we check if the road block is isActive, and that gets set before destroying
			//all collision blocks, the recursive call won't make it down here.
			world.destroyBlock(((ATileEntityBase<?>) masterBlock).position, true);
			return;
    	}
    }
    
    /**
	 *  Helper method to get the master road instance given the position of a block in the world.
	 *  This is made non-static simply to ensure people obtain a reference to an actual collision block
	 *  prior to trying to call this method, as there aren't any bound-able checks we can do on the two
	 *  input variables.
	 */
    public TileEntityRoad getMasterRoad(WrapperWorld world, Point3d position){
    	Point3d blockOffset = new Point3d();
    	Point3d testPoint = new Point3d();
    	for(int i=-TileEntityRoad.MAX_COLLISION_DISTANCE; i<2*TileEntityRoad.MAX_COLLISION_DISTANCE; ++i){
    		for(int j=-TileEntityRoad.MAX_COLLISION_DISTANCE; j<2*TileEntityRoad.MAX_COLLISION_DISTANCE; ++j){
    			for(int k=-TileEntityRoad.MAX_COLLISION_DISTANCE; k<2*TileEntityRoad.MAX_COLLISION_DISTANCE; ++k){
    				blockOffset.set(i, j, k);
    				testPoint.setTo(position).subtract(blockOffset);
            		ATileEntityBase<?> testTile = world.getTileEntity(testPoint);
            		if(testTile instanceof TileEntityRoad){
            			if(((TileEntityRoad) testTile).collisionBlockOffsets.contains(blockOffset)){
            				return (TileEntityRoad) testTile;
            			}
            		}
            	}
        	}
    	}
    	return null;
    }
    
    private static final List<BlockCollision> createCollisionBlocks(){
    	List<BlockCollision> blocks = new ArrayList<BlockCollision>();
    	for(int i=0; i<16; ++ i){
    		blocks.add(new BlockCollision(i));
    	}
    	return blocks;
    }
}
