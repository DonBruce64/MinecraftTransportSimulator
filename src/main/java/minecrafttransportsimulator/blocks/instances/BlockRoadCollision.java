package minecrafttransportsimulator.blocks.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;

public class BlockRoadCollision extends ABlockBase{
	public static List<BlockRoadCollision> blocks = createCollisionBlocks();
			
	private final int collisionHeightInPixels;
	
    public BlockRoadCollision(int collisionHeightInPixels){
    	super(10.0F, 5.0F);
    	this.collisionHeightInPixels = collisionHeightInPixels;
	}
    
    @Override
	public boolean onClicked(IWrapperWorld world, Point3i point, Axis axis, IWrapperPlayer player){
		return false;
	}
    
    @Override
    public void onBroken(IWrapperWorld world, Point3i location){
    	TileEntityRoad road = getRoadForBlock(world, location);
    	if(road != null && !road.isHolographic){
    		//We belong to this TE.  Destroy the block.  This will end up
			//destroying all collisions, including this one.  However, since
			//we check if the road block is a hologram, and that gets set before destroying
			//all collision blocks, the recurring call won't make it down here.
			world.destroyBlock(road.position);
			return;
    	}
    }
    
    public TileEntityRoad getRoadForBlock(IWrapperWorld world, Point3i location){
    	Point3i blockOffset = new Point3i(0, 0, 0);
    	Point3i testPoint = new Point3i(0, 0, 0);
    	for(int i=-TileEntityRoad.MAX_SEGMENT_LENGTH; i<2*TileEntityRoad.MAX_SEGMENT_LENGTH; ++i){
    		for(int j=-TileEntityRoad.MAX_SEGMENT_LENGTH; j<2*TileEntityRoad.MAX_SEGMENT_LENGTH; ++j){
    			for(int k=-TileEntityRoad.MAX_SEGMENT_LENGTH; k<2*TileEntityRoad.MAX_SEGMENT_LENGTH; ++k){
    				blockOffset.set(i, j, k);
    				testPoint.setTo(location).subtract(blockOffset);
            		ATileEntityBase testTile = world.getTileEntity(testPoint);
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
    
    private static final List<BlockRoadCollision> createCollisionBlocks(){
    	List<BlockRoadCollision> blocks = new ArrayList<BlockRoadCollision>();
    	for(int i=0; i<16; ++ i){
    		blocks.add(new BlockRoadCollision(i));
    	}
    	return blocks;
    }
}
