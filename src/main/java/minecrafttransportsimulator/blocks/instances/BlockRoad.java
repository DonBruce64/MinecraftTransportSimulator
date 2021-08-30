package minecrafttransportsimulator.blocks.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockRoad extends ABlockBaseTileEntity{
	
    public BlockRoad(){
    	super(10.0F, 5.0F);
	}
    
    @Override
    public void addCollisionBoxes(WrapperWorld world, Point3d position, List<BoundingBox> collidingBoxes){
    	//Get collision box from saved instance in the TE.
    	TileEntityRoad road = (TileEntityRoad) world.getTileEntity(position);
    	if(road != null){
    		collidingBoxes.add(road.boundingBox);
    	}else{
			super.addCollisionBoxes(world, position, collidingBoxes);
		}
	}
	
	@Override
    public void onBroken(WrapperWorld world, Point3d position){
		TileEntityRoad road = (TileEntityRoad) world.getTileEntity(position);
		if(road != null && road.isActive()){
			//Set the TE to inactive and remove all road connections.
			road.setActive(false);
			for(RoadLane lane : road.lanes){
				lane.removeConnections();
			}
			
			//Now remove all collision blocks.
			for(Point3d blockOffset : road.collisionBlockOffsets){
				Point3d blockLocation = position.copy().add(blockOffset);
				//Check to make sure we don't destroy non-road blocks.
				//This is required in case our TE is corrupt or someone messes with it.
				if(world.getBlock(blockLocation) instanceof BlockCollision){
					world.destroyBlock(blockLocation, true);
				}
			}
		}
	}
    
    @Override
	public TileEntityRoad createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityRoad(world, position, data);
	}
}
