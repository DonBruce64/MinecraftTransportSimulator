package minecrafttransportsimulator.blocks.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
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
	public TileEntityRoad createTileEntity(IWrapperWorld world, Point3i position, IWrapperNBT data){
		return new TileEntityRoad(world, position, data);
	}

	@Override
	public Class<TileEntityRoad> getTileEntityClass(){
		return TileEntityRoad.class;
	}
}
