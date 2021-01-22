package minecrafttransportsimulator.blocks.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityMultiblock;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockCollision extends ABlockBase{
	public static List<BlockCollision> blockInstances = createCollisionBlocks();
	private final BoundingBox blockBounds;
	
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
	public boolean onClicked(WrapperWorld world, Point3i point, Axis axis, WrapperPlayer player){
		return false;
	}
    
    @Override
    public void onBroken(WrapperWorld world, Point3i location){
    	ATileEntityMultiblock<?> masterBlock = getMasterBlock(world, location);
    	if(masterBlock != null && masterBlock.isActive()){
    		//We belong to this TE.  Destroy the block.  This will end up
			//destroying all collisions, including this one.  However, since
			//we check if the road block is isActive, and that gets set before destroying
			//all collision blocks, the recursive call won't make it down here.
			world.destroyBlock(((ATileEntityBase<?>) masterBlock).position);
			return;
    	}
    }
    
    @Override
    public void addCollisionBoxes(WrapperWorld world, Point3i location, List<BoundingBox> collidingBoxes){
		collidingBoxes.add(blockBounds);
	}
    
    /**
	 *  Helper method to get the master block instance given the position of a block in the world.
	 *  This is made non-static simply to ensure people obtain a reference to an actual collision block
	 *  prior to trying to call this method, as there aren't any bound-able checks we can do on the two
	 *  input variables.
	 */
    public ATileEntityMultiblock<?> getMasterBlock(WrapperWorld world, Point3i location){
    	Point3i blockOffset = new Point3i(0, 0, 0);
    	Point3i testPoint = new Point3i(0, 0, 0);
    	for(int i=-ATileEntityMultiblock.MAX_COLLISION_DISTANCE; i<2*ATileEntityMultiblock.MAX_COLLISION_DISTANCE; ++i){
    		for(int j=-ATileEntityMultiblock.MAX_COLLISION_DISTANCE; j<2*ATileEntityMultiblock.MAX_COLLISION_DISTANCE; ++j){
    			for(int k=-ATileEntityMultiblock.MAX_COLLISION_DISTANCE; k<2*ATileEntityMultiblock.MAX_COLLISION_DISTANCE; ++k){
    				blockOffset.set(i, j, k);
    				testPoint.setTo(location).subtract(blockOffset);
            		ATileEntityBase<?> testTile = world.getTileEntity(testPoint);
            		if(testTile instanceof ATileEntityMultiblock){
            			if(((ATileEntityMultiblock<?>) testTile).collisionBlockOffsets.contains(blockOffset)){
            				return (ATileEntityMultiblock<?>) testTile;
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
