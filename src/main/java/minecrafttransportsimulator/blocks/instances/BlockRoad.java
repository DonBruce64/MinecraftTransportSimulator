package minecrafttransportsimulator.blocks.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadLane;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;

public class BlockRoad extends ABlockBase implements IBlockTileEntity<TileEntityRoad>{
	
    public BlockRoad(){
    	super(10.0F, 5.0F);
	}
    
    @Override
    public void addCollisionBoxes(IWrapperWorld world, Point3i location, List<BoundingBox> collidingBoxes){
    	//Get collision box from saved instance in the TE.
    	TileEntityRoad road = (TileEntityRoad) world.getTileEntity(location);
    	if(road != null){
    		collidingBoxes.add(road.boundingBox);
    	}else{
			super.addCollisionBoxes(world, location, collidingBoxes);
		}
	}

	@Override
	public boolean onClicked(IWrapperWorld world, Point3i location, Axis axis, IWrapperPlayer player){
		return false;
	}
	
	@Override
    public void onBroken(IWrapperWorld world, Point3i location){
		TileEntityRoad road = (TileEntityRoad) world.getTileEntity(location);
		if(road != null && road.isActive){
			//Set the TE to inactive and remove all road connections.
			road.isActive = false;
			for(RoadLane lane : road.lanes){
				lane.removeConnections();
			}
			
			//Now remove all collision blocks.
			for(Point3i blockOffset : road.collisionBlockOffsets){
				Point3i blockLocation = location.copy().add(blockOffset);
				//Check to make sure we don't destroy non-road blocks.
				//This is required in case our TE is corrupt or someone messes with it.
				if(world.getBlock(blockLocation) instanceof BlockRoadCollision){
					world.destroyBlock(blockLocation);
				}
			}
		}
	}
    
    @Override
	public TileEntityRoad createTileEntity(IWrapperWorld world, Point3i position, IWrapperNBT data){
		return new TileEntityRoad(world, position, data);
	}

	@Override
	public Class<TileEntityRoad> getTileEntityClass(){
		return TileEntityRoad.class;
	}
}
